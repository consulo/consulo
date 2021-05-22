/*
 * Copyright 2006 Sascha Weinreuter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.intellij.plugins.intelliLang;

import com.intellij.lang.Language;
import com.intellij.openapi.command.UndoConfirmationPolicy;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.command.undo.GlobalUndoableAction;
import com.intellij.openapi.command.undo.UndoManager;
import com.intellij.openapi.command.undo.UndoableAction;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.*;
import com.intellij.psi.PsiCompiledElement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiLanguageInjectionHost;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.util.*;
import com.intellij.util.containers.ConcurrentFactoryMap;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.Convertor;
import com.intellij.util.containers.MultiMap;
import consulo.container.plugin.PluginDescriptor;
import consulo.logging.Logger;
import consulo.psi.injection.LanguageInjectionSupport;
import consulo.psi.injection.impl.ApplicationInjectionConfiguration;
import consulo.psi.injection.impl.ProjectInjectionConfiguration;
import org.intellij.plugins.intelliLang.inject.InjectorUtils;
import org.intellij.plugins.intelliLang.inject.LanguageInjectionConfigBean;
import org.intellij.plugins.intelliLang.inject.config.BaseInjection;
import org.intellij.plugins.intelliLang.inject.config.InjectionPlace;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;

/**
 * Configuration that holds configured xml tag, attribute and method parameter
 * injection settings as well as the annotations to use for injection, pattern
 * validation and for substituting non-compile time constant expression.
 * <p>
 * Making it a service may result in FileContentUtil.reparseFiles at a random loading moment which may cause
 * mysterious PSI validity losses
 */
public class Configuration implements PersistentStateComponent<Element>, ModificationTracker {
  private static final Logger LOG = Logger.getInstance(Configuration.class);

  private static final Condition<BaseInjection> LANGUAGE_INJECTION_CONDITION = o -> Language.findLanguageByID(o.getInjectedLanguageId()) != null;

  public enum InstrumentationType {
    NONE,
    ASSERT,
    EXCEPTION
  }

  public enum DfaOption {
    OFF,
    RESOLVE,
    ASSIGNMENTS,
    DFA
  }

  @Nonnull
  public static Configuration getInstance() {
    return ServiceManager.getService(ApplicationInjectionConfiguration.class);
  }

  @Nonnull
  public static Configuration getProjectInstance(Project project) {
    return ServiceManager.getService(project, ProjectInjectionConfiguration.class);
  }

  @NonNls
  public static final String COMPONENT_NAME = "LanguageInjectionConfiguration";

  // element names
  @NonNls
  private static final String INSTRUMENTATION_TYPE_NAME = "INSTRUMENTATION";
  @NonNls
  private static final String LANGUAGE_ANNOTATION_NAME = "LANGUAGE_ANNOTATION";
  @NonNls
  private static final String PATTERN_ANNOTATION_NAME = "PATTERN_ANNOTATION";
  @NonNls
  private static final String SUBST_ANNOTATION_NAME = "SUBST_ANNOTATION";
  @NonNls
  private static final String RESOLVE_REFERENCES = "RESOLVE_REFERENCES";
  @NonNls
  private static final String LOOK_FOR_VAR_ASSIGNMENTS = "LOOK_FOR_VAR_ASSIGNMENTS";
  @NonNls
  private static final String USE_DFA_IF_AVAILABLE = "USE_DFA_IF_AVAILABLE";
  @NonNls
  private static final String INCLUDE_UNCOMPUTABLES_AS_LITERALS = "INCLUDE_UNCOMPUTABLES_AS_LITERALS";
  @NonNls
  private static final String SOURCE_MODIFICATION_ALLOWED = "SOURCE_MODIFICATION_ALLOWED";

  private final Map<String, List<BaseInjection>> myInjections = ConcurrentFactoryMap.createMap(it -> ContainerUtil.createLockFreeCopyOnWriteList());

  public Collection<BaseInjection> getAllInjections() {
    ArrayList<BaseInjection> injections = new ArrayList<>();
    for (List<BaseInjection> list : myInjections.values()) {
      injections.addAll(list);
    }
    return injections;
  }

  private CachedValue<MultiMap<String, BaseInjection>> myInjectionsById = new CachedValueImpl<>(new CachedValueProvider<MultiMap<String, BaseInjection>>() {
    @Nullable
    @Override
    public Result<MultiMap<String, BaseInjection>> compute() {
      MultiMap<String, BaseInjection> map = new MultiMap<>();
      for (BaseInjection injection : getAllInjections()) {
        map.putValue(injection.getInjectedLanguageId(), injection);
      }
      return Result.create(map, Configuration.this);
    }
  });

  private volatile long myModificationCount;

  public Configuration() {
  }

  public AdvancedConfiguration getAdvancedConfiguration() {
    throw new UnsupportedOperationException("getAdvancedConfiguration should not be called");
  }

  @Override
  public void loadState(final Element element) {
    myInjections.clear();
    final Map<String, LanguageInjectionSupport> supports = new HashMap<>();
    for (LanguageInjectionSupport support : InjectorUtils.getActiveInjectionSupports()) {
      supports.put(support.getId(), support);
    }
    for (Element child : element.getChildren("injection")) {
      final String key = child.getAttributeValue("injector-id");
      final LanguageInjectionSupport support = supports.get(key);
      final BaseInjection injection = support == null ? new BaseInjection(key) : support.createInjection(child);
      injection.loadState(child);
      InjectionPlace[] places = dropKnownInvalidPlaces(injection.getInjectionPlaces());
      if (places != null) { // not all places were removed
        injection.setInjectionPlaces(places);
        myInjections.get(key).add(injection);
      }
    }
    importPlaces(getDefaultInjections());
  }

  @Nullable
  private static InjectionPlace[] dropKnownInvalidPlaces(InjectionPlace[] places) {
    InjectionPlace[] result = places;
    for (InjectionPlace place : places) {
      if (place.getText().contains("matches(\"[^${}/\\\\]+\")")) {
        result = ArrayUtil.remove(result, place);
      }
    }
    return places.length != 0 && result.length == 0 ? null : result;
  }

  private static boolean readBoolean(Element element, String key, boolean defValue) {
    final String value = JDOMExternalizerUtil.readField(element, key);
    if (value == null) {
      return defValue;
    }
    return Boolean.parseBoolean(value);
  }

  public static List<BaseInjection> loadDefaultInjections() {
    final ArrayList<Configuration> cfgList = new ArrayList<>();
    final Set<Object> visited = new HashSet<>();
    for (LanguageInjectionConfigBean configBean : LanguageInjectionSupport.CONFIG_EP_NAME.getExtensionList()) {
      PluginDescriptor descriptor = configBean.getPluginDescriptor();
      final ClassLoader loader = descriptor.getPluginClassLoader();
      try {
        final Enumeration<URL> enumeration = loader.getResources(configBean.getConfigUrl());
        if (enumeration == null || !enumeration.hasMoreElements()) {
          LOG.warn(descriptor.getPluginId() + ": " + configBean.getConfigUrl() + " was not found");
        }
        else {
          while (enumeration.hasMoreElements()) {
            URL url = enumeration.nextElement();
            if (!visited.add(url.getFile())) {
              continue; // for DEBUG mode
            }
            try {
              cfgList.add(load(url.openStream()));
            }
            catch (Exception e) {
              LOG.warn(e);
            }
          }
        }
      }
      catch (Exception e) {
        LOG.warn(e);
      }
    }

    final ArrayList<BaseInjection> defaultInjections = new ArrayList<>();
    for (String supportId : InjectorUtils.getActiveInjectionSupportIds()) {
      for (Configuration cfg : cfgList) {
        final List<BaseInjection> imported = cfg.getInjections(supportId);
        defaultInjections.addAll(imported);
      }
    }
    return defaultInjections;
  }

  @Override
  public Element getState() {
    return getState(new Element(COMPONENT_NAME));
  }

  protected Element getState(final Element element) {
    Comparator<BaseInjection> comparator = new Comparator<BaseInjection>() {
      @Override
      public int compare(final BaseInjection o1, final BaseInjection o2) {
        return Comparing.compare(o1.getDisplayName(), o2.getDisplayName());
      }
    };
    List<String> injectorIds = new ArrayList<>(myInjections.keySet());
    Collections.sort(injectorIds);
    for (String key : injectorIds) {
      TreeSet<BaseInjection> injections = new TreeSet<>(comparator);
      injections.addAll(myInjections.get(key));
      injections.removeAll(getDefaultInjections());
      for (BaseInjection injection : injections) {
        element.addContent(injection.getState());
      }
    }
    return element;
  }

  public List<BaseInjection> getDefaultInjections() {
    return Collections.emptyList();
  }

  public Collection<BaseInjection> getInjectionsByLanguageId(String languageId) {
    return myInjectionsById.getValue().get(languageId);
  }

  @Nullable
  public static Configuration load(final InputStream is) throws IOException, JDOMException {
    try {
      final Document document = JDOMUtil.loadDocument(is);
      final ArrayList<Element> elements = new ArrayList<>();
      final Element rootElement = document.getRootElement();
      final Element state;
      if (rootElement.getName().equals(COMPONENT_NAME)) {
        state = rootElement;
      }
      else {
        elements.add(rootElement);
        //noinspection unchecked
        elements.addAll(rootElement.getChildren("component"));
        state = ContainerUtil.find(elements, new Condition<Element>() {
          @Override
          public boolean value(final Element element) {
            return "component".equals(element.getName()) && COMPONENT_NAME.equals(element.getAttributeValue("name"));
          }
        });
      }
      if (state != null) {
        final Configuration cfg = new Configuration();
        cfg.loadState(state);
        return cfg;
      }
      return null;
    }
    finally {
      is.close();
    }
  }

  private int importPlaces(final List<BaseInjection> injections) {
    final Map<String, Set<BaseInjection>> map = ContainerUtil.classify(injections.iterator(), new Convertor<BaseInjection, String>() {
      @Override
      public String convert(final BaseInjection o) {
        return o.getSupportId();
      }
    });
    final ArrayList<BaseInjection> originalInjections = new ArrayList<>();
    final ArrayList<BaseInjection> newInjections = new ArrayList<>();
    for (String supportId : InjectorUtils.getActiveInjectionSupportIds()) {
      final Set<BaseInjection> importingInjections = map.get(supportId);
      if (importingInjections == null) {
        continue;
      }
      importInjections(getInjections(supportId), importingInjections, originalInjections, newInjections);
    }
    if (!newInjections.isEmpty()) {
      configurationModified();
    }
    replaceInjections(newInjections, originalInjections, true);
    return newInjections.size();
  }

  static void importInjections(final Collection<BaseInjection> existingInjections,
                               final Collection<BaseInjection> importingInjections,
                               final Collection<BaseInjection> originalInjections,
                               final Collection<BaseInjection> newInjections) {
    final MultiValuesMap<InjectionPlace, BaseInjection> placeMap = new MultiValuesMap<>();
    for (BaseInjection exising : existingInjections) {
      for (InjectionPlace place : exising.getInjectionPlaces()) {
        placeMap.put(place, exising);
      }
    }
    main:
    for (BaseInjection other : importingInjections) {
      final List<BaseInjection> matchingInjections = ContainerUtil.concat(other.getInjectionPlaces(), new Function<InjectionPlace, Collection<? extends BaseInjection>>() {
        @Override
        public Collection<? extends BaseInjection> fun(final InjectionPlace o) {
          final Collection<BaseInjection> collection = placeMap.get(o);
          return collection == null ? Collections.<BaseInjection>emptyList() : collection;
        }
      });
      if (matchingInjections.isEmpty()) {
        newInjections.add(other);
      }
      else {
        BaseInjection existing = null;
        for (BaseInjection injection : matchingInjections) {
          if (injection.equals(other)) {
            continue main;
          }
          if (existing == null && injection.sameLanguageParameters(other)) {
            existing = injection;
          }
        }
        if (existing == null) {
          continue main; // skip!! language changed
        }
        final BaseInjection newInjection = existing.copy();
        newInjection.mergeOriginalPlacesFrom(other, true);
        if (!newInjection.equals(existing)) {
          originalInjections.add(existing);
          newInjections.add(newInjection);
        }
      }
    }
  }

  private void configurationModified() {
    myModificationCount++;
  }

  @Override
  public long getModificationCount() {
    return myModificationCount;
  }

  @Nullable
  public BaseInjection findExistingInjection(@Nonnull final BaseInjection injection) {
    final List<BaseInjection> list = getInjections(injection.getSupportId());
    for (BaseInjection cur : list) {
      if (cur.intersectsWith(injection)) {
        return cur;
      }
    }
    return null;
  }

  public boolean setHostInjectionEnabled(final PsiLanguageInjectionHost host, final Collection<String> languages, final boolean enabled) {
    final ArrayList<BaseInjection> originalInjections = new ArrayList<>();
    final ArrayList<BaseInjection> newInjections = new ArrayList<>();
    for (LanguageInjectionSupport support : InjectorUtils.getActiveInjectionSupports()) {
      for (BaseInjection injection : getInjections(support.getId())) {
        if (!languages.contains(injection.getInjectedLanguageId())) {
          continue;
        }
        boolean replace = false;
        final ArrayList<InjectionPlace> newPlaces = new ArrayList<>();
        for (InjectionPlace place : injection.getInjectionPlaces()) {
          if (place.isEnabled() != enabled && place.getElementPattern() != null && (place.getElementPattern().accepts(host) || place.getElementPattern().accepts(host.getParent()))) {
            newPlaces.add(place.enabled(enabled));
            replace = true;
          }
          else {
            newPlaces.add(place);
          }
        }
        if (replace) {
          originalInjections.add(injection);
          final BaseInjection newInjection = injection.copy();
          newInjection.setInjectionPlaces(newPlaces.toArray(new InjectionPlace[newPlaces.size()]));
          newInjections.add(newInjection);
        }
      }
    }
    if (!originalInjections.isEmpty()) {
      replaceInjectionsWithUndo(host.getProject(), newInjections, originalInjections, Collections.<PsiElement>emptyList());
      return true;
    }
    return false;
  }

  protected void setInjections(Collection<BaseInjection> injections) {
    for (BaseInjection injection : injections) {
      myInjections.get(injection.getSupportId()).add(injection);
    }
  }

  /**
   * @param injectorId see {@link LanguageInjectionSupport#getId()}
   */
  @Nonnull
  public List<BaseInjection> getInjections(final String injectorId) {
    return Collections.unmodifiableList(myInjections.get(injectorId));
  }

  public void replaceInjectionsWithUndo(final Project project,
                                        final List<? extends BaseInjection> newInjections,
                                        final List<? extends BaseInjection> originalInjections,
                                        final List<? extends PsiElement> psiElementsToRemove) {
    replaceInjectionsWithUndo(project, newInjections, originalInjections, psiElementsToRemove, new PairProcessor<List<? extends BaseInjection>, List<? extends BaseInjection>>() {
      @Override
      public boolean process(final List<? extends BaseInjection> add, final List<? extends BaseInjection> remove) {
        replaceInjectionsWithUndoInner(add, remove);
        if (ContainerUtil.find(add, LANGUAGE_INJECTION_CONDITION) != null || ContainerUtil.find(remove, LANGUAGE_INJECTION_CONDITION) != null) {
          FileContentUtil.reparseOpenedFiles();
        }
        return true;
      }
    });
  }

  protected void replaceInjectionsWithUndoInner(final List<? extends BaseInjection> add, final List<? extends BaseInjection> remove) {
    replaceInjections(add, remove, false);
  }

  public static <T> void replaceInjectionsWithUndo(final Project project,
                                                   final T add,
                                                   final T remove,
                                                   final List<? extends PsiElement> psiElementsToRemove,
                                                   final PairProcessor<T, T> actualProcessor) {
    final UndoableAction action = new GlobalUndoableAction() {
      @Override
      public void undo() {
        actualProcessor.process(remove, add);
      }

      @Override
      public void redo() {
        actualProcessor.process(add, remove);
      }
    };
    final List<PsiFile> psiFiles = ContainerUtil.mapNotNull(psiElementsToRemove, new NullableFunction<PsiElement, PsiFile>() {
      @Override
      public PsiFile fun(final PsiElement psiAnnotation) {
        return psiAnnotation instanceof PsiCompiledElement ? null : psiAnnotation.getContainingFile();
      }
    });
    new WriteCommandAction.Simple(project, "Language Injection Configuration Update", PsiUtilCore.toPsiFileArray(psiFiles)) {
      @Override
      public void run() {
        for (PsiElement annotation : psiElementsToRemove) {
          annotation.delete();
        }
        actualProcessor.process(add, remove);
        UndoManager.getInstance(project).undoableActionPerformed(action);
      }

      @Override
      protected UndoConfirmationPolicy getUndoConfirmationPolicy() {
        return UndoConfirmationPolicy.REQUEST_CONFIRMATION;
      }
    }.execute();
  }

  public boolean replaceInjections(List<? extends BaseInjection> newInjections, List<? extends BaseInjection> originalInjections, boolean forceLevel) {
    boolean changed = false;
    for (BaseInjection injection : originalInjections) {
      changed |= myInjections.get(injection.getSupportId()).remove(injection);
    }
    for (BaseInjection injection : newInjections) {
      changed |= myInjections.get(injection.getSupportId()).add(injection);
    }
    if (changed) {
      configurationModified();
    }
    return changed;
  }

  public static class AdvancedConfiguration {
    // runtime pattern validation instrumentation
    @Nonnull
    private InstrumentationType myInstrumentationType = InstrumentationType.ASSERT;

    // annotation class names
    @Nonnull
    private String myLanguageAnnotation;
    @Nonnull
    private String myPatternAnnotation;
    @Nonnull
    private String mySubstAnnotation;

    private boolean myIncludeUncomputablesAsLiterals;
    private DfaOption myDfaOption = DfaOption.RESOLVE;
    private boolean mySourceModificationAllowed;

    // cached annotation name pairs
    private Pair<String, ? extends Set<String>> myLanguageAnnotationPair;
    private Pair<String, ? extends Set<String>> myPatternAnnotationPair;

    private Pair<String, ? extends Set<String>> mySubstAnnotationPair;

    public AdvancedConfiguration() {
      setLanguageAnnotation("org.intellij.lang.annotations.Language");
      setPatternAnnotation("org.intellij.lang.annotations.Pattern");
      setSubstAnnotation("org.intellij.lang.annotations.Subst");
    }

    public String getLanguageAnnotationClass() {
      return myLanguageAnnotation;
    }

    public String getPatternAnnotationClass() {
      return myPatternAnnotation;
    }

    public String getSubstAnnotationClass() {
      return mySubstAnnotation;
    }

    public void setInstrumentationType(@Nullable String type) {
      if (type != null) {
        setInstrumentationType(InstrumentationType.valueOf(type));
      }
    }

    public void setInstrumentationType(@Nonnull InstrumentationType type) {
      myInstrumentationType = type;
    }

    public void setLanguageAnnotation(@Nullable String languageAnnotation) {
      if (languageAnnotation == null) {
        return;
      }
      myLanguageAnnotation = languageAnnotation;
      myLanguageAnnotationPair = Pair.create(languageAnnotation, Collections.singleton(languageAnnotation));
    }

    public Pair<String, ? extends Set<String>> getLanguageAnnotationPair() {
      return myLanguageAnnotationPair;
    }

    public void setPatternAnnotation(@Nullable String patternAnnotation) {
      if (patternAnnotation == null) {
        return;
      }
      myPatternAnnotation = patternAnnotation;
      myPatternAnnotationPair = Pair.create(patternAnnotation, Collections.singleton(patternAnnotation));
    }

    public Pair<String, ? extends Set<String>> getPatternAnnotationPair() {
      return myPatternAnnotationPair;
    }

    public void setSubstAnnotation(@Nullable String substAnnotation) {
      if (substAnnotation == null) {
        return;
      }
      mySubstAnnotation = substAnnotation;
      mySubstAnnotationPair = Pair.create(substAnnotation, Collections.singleton(substAnnotation));
    }

    public Pair<String, ? extends Set<String>> getSubstAnnotationPair() {
      return mySubstAnnotationPair;
    }

    public boolean isIncludeUncomputablesAsLiterals() {
      return myIncludeUncomputablesAsLiterals;
    }

    public void setIncludeUncomputablesAsLiterals(boolean flag) {
      myIncludeUncomputablesAsLiterals = flag;
    }

    @Nonnull
    public DfaOption getDfaOption() {
      return myDfaOption;
    }

    public void setDfaOption(@Nonnull final DfaOption dfaOption) {
      myDfaOption = dfaOption;
    }

    public boolean isSourceModificationAllowed() {
      return mySourceModificationAllowed;
    }

    public void setSourceModificationAllowed(boolean sourceModificationAllowed) {
      mySourceModificationAllowed = sourceModificationAllowed;
    }

    public InstrumentationType getInstrumentation() {
      return myInstrumentationType;
    }

    public void writeState(final Element element) {
      JDOMExternalizerUtil.writeField(element, INSTRUMENTATION_TYPE_NAME, myInstrumentationType.toString());
      JDOMExternalizerUtil.writeField(element, LANGUAGE_ANNOTATION_NAME, myLanguageAnnotation);
      JDOMExternalizerUtil.writeField(element, PATTERN_ANNOTATION_NAME, myPatternAnnotation);
      JDOMExternalizerUtil.writeField(element, SUBST_ANNOTATION_NAME, mySubstAnnotation);
      if (myIncludeUncomputablesAsLiterals) {
        JDOMExternalizerUtil.writeField(element, INCLUDE_UNCOMPUTABLES_AS_LITERALS, "true");
      }
      if (mySourceModificationAllowed) {
        JDOMExternalizerUtil.writeField(element, SOURCE_MODIFICATION_ALLOWED, "true");
      }
      switch (myDfaOption) {
        case OFF:
          break;
        case RESOLVE:
          JDOMExternalizerUtil.writeField(element, RESOLVE_REFERENCES, Boolean.TRUE.toString());
          break;
        case ASSIGNMENTS:
          JDOMExternalizerUtil.writeField(element, LOOK_FOR_VAR_ASSIGNMENTS, Boolean.TRUE.toString());
          break;
        case DFA:
          JDOMExternalizerUtil.writeField(element, USE_DFA_IF_AVAILABLE, Boolean.TRUE.toString());
          break;
      }
    }

    public void loadState(final Element element) {
      setInstrumentationType(JDOMExternalizerUtil.readField(element, INSTRUMENTATION_TYPE_NAME));
      setLanguageAnnotation(JDOMExternalizerUtil.readField(element, LANGUAGE_ANNOTATION_NAME));
      setPatternAnnotation(JDOMExternalizerUtil.readField(element, PATTERN_ANNOTATION_NAME));
      setSubstAnnotation(JDOMExternalizerUtil.readField(element, SUBST_ANNOTATION_NAME));
      if (readBoolean(element, RESOLVE_REFERENCES, true)) {
        setDfaOption(DfaOption.RESOLVE);
      }
      if (readBoolean(element, LOOK_FOR_VAR_ASSIGNMENTS, false)) {
        setDfaOption(DfaOption.ASSIGNMENTS);
      }
      if (readBoolean(element, USE_DFA_IF_AVAILABLE, false)) {
        setDfaOption(DfaOption.DFA);
      }
      setIncludeUncomputablesAsLiterals(readBoolean(element, INCLUDE_UNCOMPUTABLES_AS_LITERALS, false));
      setSourceModificationAllowed(readBoolean(element, SOURCE_MODIFICATION_ALLOWED, false));
    }
  }
}
