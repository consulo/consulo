// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.util.gotoByName;

import com.intellij.ide.DataManager;
import com.intellij.ide.SearchTopHitProvider;
import com.intellij.ide.actions.ApplyIntentionAction;
import com.intellij.ide.ui.search.ActionFromOptionDescriptorProvider;
import com.intellij.ide.ui.search.OptionDescription;
import com.intellij.ide.ui.search.SearchableOptionsRegistrar;
import com.intellij.ide.ui.search.SearchableOptionsRegistrarImpl;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.impl.ActionManagerImpl;
import com.intellij.openapi.application.ReadAction;
import consulo.logging.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.codeStyle.NameUtil;
import com.intellij.psi.codeStyle.WordPrefixMatcher;
import com.intellij.ui.switcher.QuickActionProvider;
import com.intellij.util.CollectConsumer;
import com.intellij.util.Processor;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.JBIterable;
import com.intellij.util.text.Matcher;
import gnu.trove.THashSet;
import javax.annotation.Nonnull;

import java.util.*;

import static com.intellij.ide.util.gotoByName.GotoActionModel.*;

/**
 * @author peter
 */
public class GotoActionItemProvider implements ChooseByNameItemProvider {
  private final ActionManager myActionManager = ActionManager.getInstance();
  private final GotoActionModel myModel;
  private final NotNullLazyValue<Map<String, ApplyIntentionAction>> myIntentions;

  public GotoActionItemProvider(GotoActionModel model) {
    myModel = model;
    myIntentions = NotNullLazyValue.createValue(() -> ReadAction.compute(() -> myModel.getAvailableIntentions()));
  }

  @Nonnull
  @Override
  public List<String> filterNames(@Nonnull ChooseByNameBase base, @Nonnull String[] names, @Nonnull String pattern) {
    return Collections.emptyList(); // no common prefix insertion in goto action
  }

  @Override
  public boolean filterElements(@Nonnull final ChooseByNameBase base,
                                @Nonnull final String pattern,
                                boolean everywhere,
                                @Nonnull ProgressIndicator cancelled,
                                @Nonnull final Processor<Object> consumer) {
    return filterElements(pattern, value -> {
      if (!everywhere && value.value instanceof GotoActionModel.ActionWrapper && !((ActionWrapper)value.value).isAvailable()) {
        return true;
      }
      return consumer.process(value);
    });
  }

  public boolean filterElements(@Nonnull String pattern, @Nonnull Processor<? super MatchedValue> consumer) {
    DataContext dataContext = DataManager.getInstance().getDataContext(myModel.getContextComponent());

    if (!processAbbreviations(pattern, consumer, dataContext)) return false;
    if (!processIntentions(pattern, consumer, dataContext)) return false;
    if (!processActions(pattern, consumer, dataContext)) return false;
    if (Registry.is("goto.action.skip.tophits.and.options")) return true;
    if (!processTopHits(pattern, consumer, dataContext)) return false;
    if (!processOptions(pattern, consumer, dataContext)) return false;

    return true;
  }

  private boolean processAbbreviations(@Nonnull String pattern, Processor<? super MatchedValue> consumer, DataContext context) {
    List<String> actionIds = AbbreviationManager.getInstance().findActions(pattern);
    JBIterable<MatchedValue> wrappers = JBIterable.from(actionIds).filterMap(myActionManager::getAction).transform(action -> {
      ActionWrapper wrapper = wrapAnAction(action, context);
      return new MatchedValue(wrapper, pattern) {
        @Nonnull
        @Override
        public String getValueText() {
          return pattern;
        }
      };
    });
    return processItems(pattern, wrappers, consumer);
  }

  private boolean processTopHits(String pattern, Processor<? super MatchedValue> consumer, DataContext dataContext) {
    Project project = dataContext.getData(CommonDataKeys.PROJECT);
    final CollectConsumer<Object> collector = new CollectConsumer<Object>();
    for (SearchTopHitProvider provider : SearchTopHitProvider.EP_NAME.getExtensionList()) {
      if (provider instanceof com.intellij.ide.ui.OptionsTopHitProvider.CoveredByToggleActions) continue;
      if (provider instanceof com.intellij.ide.ui.OptionsTopHitProvider && !((com.intellij.ide.ui.OptionsTopHitProvider)provider).isEnabled(project)) continue;
      if (provider instanceof com.intellij.ide.ui.OptionsTopHitProvider && !StringUtil.startsWith(pattern, "#")) {
        String prefix = "#" + ((com.intellij.ide.ui.OptionsTopHitProvider)provider).getId() + " ";
        provider.consumeTopHits(prefix + pattern, collector, project);
      }
      provider.consumeTopHits(pattern, collector, project);
    }
    final Collection<Object> result = collector.getResult();
    final List<Comparable> c = new ArrayList<Comparable>();
    for (Object o : result) {
      if (o instanceof Comparable) {
        c.add((Comparable)o);
      }
    }
    return processItems(pattern, JBIterable.from(c), consumer);
  }

  private boolean processOptions(String pattern, Processor<? super MatchedValue> consumer, DataContext dataContext) {
    Map<String, String> map = myModel.getConfigurablesNames();
    SearchableOptionsRegistrarImpl registrar = (SearchableOptionsRegistrarImpl)SearchableOptionsRegistrar.getInstance();

    List<Object> options = new ArrayList<>();
    final Set<String> words = registrar.getProcessedWords(pattern);
    Set<OptionDescription> optionDescriptions = null;
    final String actionManagerName = "ActionManager";
    for (String word : words) {
      final Set<OptionDescription> descriptions = registrar.getAcceptableDescriptions(word);
      if (descriptions != null) {
        descriptions.removeIf(description -> actionManagerName.equals(description.getPath()));
        if (!descriptions.isEmpty()) {
          if (optionDescriptions == null) {
            optionDescriptions = descriptions;
          }
          else {
            optionDescriptions.retainAll(descriptions);
          }
        }
      }
      else {
        optionDescriptions = null;
        break;
      }
    }
    if (!StringUtil.isEmptyOrSpaces(pattern)) {
      Matcher matcher = buildMatcher(pattern);
      if (optionDescriptions == null) optionDescriptions = new HashSet<>();
      for (Map.Entry<String, String> entry : map.entrySet()) {
        if (matcher.matches(entry.getValue())) {
          optionDescriptions.add(new OptionDescription(null, entry.getKey(), entry.getValue(), null, entry.getValue()));
        }
      }
    }
    if (optionDescriptions != null && !optionDescriptions.isEmpty()) {
      Set<String> currentHits = new HashSet<>();
      for (Iterator<OptionDescription> iterator = optionDescriptions.iterator(); iterator.hasNext(); ) {
        OptionDescription description = iterator.next();
        final String hit = description.getHit();
        if (hit == null || !currentHits.add(hit.trim())) {
          iterator.remove();
        }
      }
      for (OptionDescription description : optionDescriptions) {
        for (ActionFromOptionDescriptorProvider converter : ActionFromOptionDescriptorProvider.EP.getExtensionList()) {
          AnAction action = converter.provide(description);
          if (action != null) options.add(new ActionWrapper(action, null, MatchMode.NAME, dataContext, myModel));
          options.add(description);
        }
      }
    }
    return processItems(pattern, JBIterable.from(options), consumer);
  }

  private boolean processActions(String pattern, Processor<? super MatchedValue> consumer, DataContext dataContext) {
    Set<String> ids = ((ActionManagerImpl)myActionManager).getActionIds();
    JBIterable<AnAction> actions = JBIterable.from(ids).filterMap(myActionManager::getAction);
    Matcher matcher = buildMatcher(pattern);

    QuickActionProvider provider = dataContext.getData(QuickActionProvider.KEY);
    if (provider != null) {
      actions = actions.append(provider.getActions(true));
    }

    JBIterable<ActionWrapper> actionWrappers = actions.unique().filterMap(action -> {
      MatchMode mode = myModel.actionMatches(pattern, matcher, action);
      if (mode == MatchMode.NONE) return null;
      return new ActionWrapper(action, myModel.getGroupMapping(action), mode, dataContext, myModel);
    });
    return processItems(pattern, actionWrappers, consumer);
  }

  @Nonnull
  static Matcher buildMatcher(String pattern) {
    return pattern.contains(" ") ? new WordPrefixMatcher(pattern) : NameUtil.buildMatcher("*" + pattern, NameUtil.MatchingCaseSensitivity.NONE);
  }

  private boolean processIntentions(String pattern, Processor<? super MatchedValue> consumer, DataContext dataContext) {
    Matcher matcher = buildMatcher(pattern);
    Map<String, ApplyIntentionAction> intentionMap = myIntentions.getValue();
    JBIterable<ActionWrapper> intentions = JBIterable.from(intentionMap.keySet()).filterMap(intentionText -> {
      ApplyIntentionAction intentionAction = intentionMap.get(intentionText);
      if (myModel.actionMatches(pattern, matcher, intentionAction) == MatchMode.NONE) return null;
      GroupMapping groupMapping = GroupMapping.createFromText(intentionText);
      return new ActionWrapper(intentionAction, groupMapping, MatchMode.INTENTION, dataContext, myModel);
    });
    return processItems(pattern, intentions, consumer);
  }

  @Nonnull
  private ActionWrapper wrapAnAction(@Nonnull AnAction action, DataContext dataContext) {
    return new ActionWrapper(action, myModel.getGroupMapping(action), MatchMode.NAME, dataContext, myModel);
  }

  private static final Logger LOG = Logger.getInstance(GotoActionItemProvider.class);

  private static boolean processItems(String pattern, JBIterable<?> items, Processor<? super MatchedValue> consumer) {
    List<MatchedValue> matched = ContainerUtil.newArrayList(items.map(o -> o instanceof MatchedValue ? (MatchedValue)o : new MatchedValue(o, pattern)));
    try {
      Collections.sort(matched, (o1, o2) -> o1.compareWeights(o2));
    }
    catch (IllegalArgumentException e) {
      LOG.error("Comparison method violates its general contract with pattern '" + pattern + "'", e);
    }
    return ContainerUtil.process(matched, consumer);
  }
}
