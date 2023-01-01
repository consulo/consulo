/*
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.language.editor.inspection.scheme;

import consulo.annotation.access.RequiredReadAction;
import consulo.language.Language;
import consulo.language.editor.inspection.BatchSuppressableTool;
import consulo.language.editor.inspection.InspectionSuppressor;
import consulo.language.editor.inspection.InspectionsBundle;
import consulo.language.editor.inspection.SuppressQuickFix;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.file.FileViewProvider;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiUtilCore;
import consulo.language.template.TemplateLanguageFileViewProvider;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.ContainerUtil;
import consulo.util.io.ResourceUtil;
import consulo.util.lang.StringUtil;
import consulo.util.xml.serializer.*;
import org.jdom.Element;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;

/**
 * @author anna
 * @since 28-Nov-2005
 */
public abstract class InspectionProfileEntry implements BatchSuppressableTool {
  public static final String GENERAL_GROUP_NAME = InspectionsBundle.message("inspection.general.tools.group.name");

  private static final Logger LOG = Logger.getInstance(InspectionProfileEntry.class);

  private static final SkipDefaultValuesSerializationFilters DEFAULT_FILTER = new SkipDefaultValuesSerializationFilters();
  private static Set<String> ourBlackList = null;
  private static final Object BLACK_LIST_LOCK = new Object();
  private Boolean myUseNewSerializer = null;

  @Nullable
  public Language getLanguage() {
    return null;
  }

  @Nullable
  public String getAlternativeID() {
    return null;
  }

  @Override
  public boolean isSuppressedFor(@Nonnull PsiElement element) {
    for (InspectionSuppressor suppressor : getSuppressors(element)) {
      String toolId = getSuppressId();
      if (suppressor.isSuppressedFor(element, toolId)) {
        return true;
      }
      final String alternativeId = getAlternativeID();
      if (alternativeId != null && !alternativeId.equals(toolId) && suppressor.isSuppressedFor(element, alternativeId)) {
        return true;
      }
    }
    return false;
  }

  protected String getSuppressId() {
    return getShortName();
  }

  @Nonnull
  @Override
  public SuppressQuickFix[] getBatchSuppressActions(@Nullable PsiElement element) {
    if (element != null) {
      SuppressQuickFix[] quickFixes = SuppressQuickFix.EMPTY_ARRAY;
      for (InspectionSuppressor suppressor : getSuppressors(element)) {
        quickFixes = ArrayUtil.mergeArrays(quickFixes, suppressor.getSuppressActions(element, getShortName()));
      }
      return quickFixes;
    }
    return SuppressQuickFix.EMPTY_ARRAY;
  }

  @RequiredReadAction
  @Nonnull
  public static Set<InspectionSuppressor> getSuppressors(@Nonnull PsiElement element) {
    PsiUtilCore.ensureValid(element);
    PsiFile file = element.getContainingFile();
    if (file == null) {
      return Collections.emptySet();
    }
    FileViewProvider viewProvider = file.getViewProvider();
    final List<InspectionSuppressor> elementLanguageSuppressor = InspectionSuppressor.forLanguage(element.getLanguage());
    if (viewProvider instanceof TemplateLanguageFileViewProvider) {
      Set<InspectionSuppressor> suppressors = new LinkedHashSet<>();
      ContainerUtil.addAllNotNull(suppressors, InspectionSuppressor.forLanguage(viewProvider.getBaseLanguage()));
      for (Language language : viewProvider.getLanguages()) {
        ContainerUtil.addAllNotNull(suppressors, InspectionSuppressor.forLanguage(language));
      }
      ContainerUtil.addAllNotNull(suppressors, elementLanguageSuppressor);
      return suppressors;
    }
    if (!element.getLanguage().isKindOf(viewProvider.getBaseLanguage())) {
      Set<InspectionSuppressor> suppressors = new LinkedHashSet<>();
      ContainerUtil.addAllNotNull(suppressors, InspectionSuppressor.forLanguage(viewProvider.getBaseLanguage()));
      ContainerUtil.addAllNotNull(suppressors, elementLanguageSuppressor);
      return suppressors;
    }
    int size = elementLanguageSuppressor.size();
    switch (size) {
      case 0:
        return Collections.emptySet();
      case 1:
        return Collections.singleton(elementLanguageSuppressor.get(0));
      default:
        return new HashSet<>(elementLanguageSuppressor);
    }
  }

  public void cleanup(Project project) {
  }

  @Nonnull
  public abstract String getGroupDisplayName();

  @Nonnull
  public String[] getGroupPath() {
    String groupDisplayName = getGroupDisplayName();
    if (groupDisplayName.isEmpty()) {
      groupDisplayName = GENERAL_GROUP_NAME;
    }

    Language language = getLanguage();
    if (language != null) {
      return new String[]{language.getDisplayName(), groupDisplayName};
    }
    else {
      return new String[]{groupDisplayName};
    }
  }

  @Nonnull
  public abstract String getDisplayName();

  @Nonnull
  public String getShortName() {
    return getShortName(getClass().getSimpleName());
  }

  @Nonnull
  public static String getShortName(@Nonnull String className) {
    return StringUtil.trimEnd(StringUtil.trimEnd(className, "Inspection"), "InspectionBase");
  }

  @Nonnull
  public abstract HighlightDisplayLevel getDefaultLevel();

  public boolean isEnabledByDefault() {
    return true;
  }

  /**
   * This method is called each time UI is shown.
   *
   * @return null if no UI options required.
   */
  @Nullable
  public JComponent createOptionsPanel() {
    return null;
  }

  /**
   * Read in settings from XML config.
   * Default implementation uses XmlSerializer so you may use public fields (like <code>int TOOL_OPTION</code>)
   * and bean-style getters/setters (like <code>int getToolOption(), void setToolOption(int)</code>) to store your options.
   *
   * @param node to read settings from.
   * @throws InvalidDataException if the loaded data was not valid.
   */
  @SuppressWarnings("deprecation")
  public void readSettings(@Nonnull Element node) throws InvalidDataException {
    if (useNewSerializer()) {
      try {
        XmlSerializer.deserializeInto(this, node);
      }
      catch (XmlSerializationException e) {
        throw new InvalidDataException(e);
      }
    }
    else {
      //noinspection UnnecessaryFullyQualifiedName
      DefaultJDOMExternalizer.readExternal(this, node);
    }
  }

  /**
   * Store current settings in XML config.
   * Default implementation uses XmlSerializer so you may use public fields (like <code>int TOOL_OPTION</code>)
   * and bean-style getters/setters (like <code>int getToolOption(), void setToolOption(int)</code>) to store your options.
   *
   * @param node to store settings to.
   * @throws WriteExternalException if no data should be saved for this component.
   */
  @SuppressWarnings("deprecation")
  public void writeSettings(@Nonnull Element node) throws WriteExternalException {
    if (useNewSerializer()) {
      XmlSerializer.serializeInto(this, node, getSerializationFilter());
    }
    else {
      //noinspection UnnecessaryFullyQualifiedName
      DefaultJDOMExternalizer.writeExternal(this, node);
    }
  }

  private synchronized boolean useNewSerializer() {
    if (myUseNewSerializer == null) {
      myUseNewSerializer = !getBlackList().contains(getClass().getName());
    }
    return myUseNewSerializer;
  }

  private static void loadBlackList() {
    ourBlackList = new HashSet<>();

    final URL url = InspectionProfileEntry.class.getResource("inspection-black-list.txt");
    if (url == null) {
      LOG.error("Resource not found");
      return;
    }

    try {
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()))) {
        String line;
        while ((line = reader.readLine()) != null) {
          line = line.trim();
          if (!line.isEmpty()) ourBlackList.add(line);
        }
      }
    }
    catch (IOException e) {
      LOG.error("Unable to load resource: " + url, e);
    }
  }

  @Nonnull
  public static Collection<String> getBlackList() {
    synchronized (BLACK_LIST_LOCK) {
      if (ourBlackList == null) {
        loadBlackList();
      }
      return ourBlackList;
    }
  }

  /**
   * Returns filter used to omit default values on saving inspection settings.
   * Default implementation uses SkipDefaultValuesSerializationFilters.
   *
   * @return serialization filter.
   */
  @SuppressWarnings("MethodMayBeStatic")
  @Nullable
  protected SerializationFilter getSerializationFilter() {
    return DEFAULT_FILTER;
  }

  /**
   * Override this method to return a html inspection description. Otherwise it will be loaded from resources using ID.
   *
   * @return hard-code inspection description.
   */
  @Nullable
  public String getStaticDescription() {
    return null;
  }

  @Nullable
  public String getDescriptionFileName() {
    return null;
  }

  @Nullable
  protected URL getDescriptionUrl() {
    final String fileName = getDescriptionFileName();
    if (fileName == null) return null;
    return ResourceUtil.getResource(getDescriptionContextClass(), "/inspectionDescriptions", fileName);
  }

  @Nonnull
  protected Class<? extends InspectionProfileEntry> getDescriptionContextClass() {
    return getClass();
  }

  public boolean isInitialized() {
    return true;
  }

  /**
   * @return short name of tool whose results will be used
   */
  @Nullable
  public String getMainToolId() {
    return null;
  }

  @Nullable
  public String loadDescription() {
    final String description = getStaticDescription();
    if (description != null) return description;

    try {
      URL descriptionUrl = getDescriptionUrl();
      if (descriptionUrl == null) return null;
      return ResourceUtil.loadText(descriptionUrl);
    }
    catch (IOException ignored) {
    }

    return null;
  }

  public static String getGeneralGroupName() {
    return InspectionsBundle.message("inspection.general.tools.group.name");
  }
}
