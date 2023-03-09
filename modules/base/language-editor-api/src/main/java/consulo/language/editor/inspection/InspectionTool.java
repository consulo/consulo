/*
 * Copyright 2013-2023 consulo.io
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
package consulo.language.editor.inspection;

import consulo.language.Language;
import consulo.language.editor.internal.inspection.DummyInspectionToolState;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.psi.PsiElement;
import consulo.project.Project;
import consulo.util.collection.ArrayUtil;
import consulo.util.io.ResourceUtil;
import consulo.util.lang.StringUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.net.URL;

/**
 * @author VISTALL
 * @since 04/03/2023
 */
public abstract class InspectionTool implements BatchSuppressableTool {
  @Nonnull
  public InspectionToolState<?> createStateProvider() {
    return DummyInspectionToolState.INSTANCE;
  }

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
    for (InspectionSuppressor suppressor : SuppressionUtil.getSuppressors(element)) {
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
      for (InspectionSuppressor suppressor : SuppressionUtil.getSuppressors(element)) {
        quickFixes = ArrayUtil.mergeArrays(quickFixes, suppressor.getSuppressActions(element, getShortName()));
      }
      return quickFixes;
    }
    return SuppressQuickFix.EMPTY_ARRAY;
  }

  public void cleanup(Project project) {
  }

  @Nonnull
  public abstract String getGroupDisplayName();

  @Nonnull
  public String[] getGroupPath() {
    String groupDisplayName = getGroupDisplayName();
    if (groupDisplayName.isEmpty()) {
      groupDisplayName = InspectionsBundle.message("inspection.general.tools.group.name");
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
  protected Class<? extends InspectionTool> getDescriptionContextClass() {
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
