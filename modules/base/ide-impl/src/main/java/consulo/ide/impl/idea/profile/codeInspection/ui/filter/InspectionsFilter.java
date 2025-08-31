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
package consulo.ide.impl.idea.profile.codeInspection.ui.filter;

import consulo.language.editor.inspection.scheme.GlobalInspectionToolWrapper;
import consulo.language.editor.inspection.scheme.InspectionToolWrapper;
import consulo.language.editor.internal.inspection.ScopeToolState;
import consulo.language.editor.inspection.scheme.Tools;
import consulo.language.Language;
import consulo.language.editor.annotation.HighlightSeverity;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Dmitry Batkovich
 */
public abstract class InspectionsFilter {

  private final Set<HighlightSeverity> mySuitableSeverities = new HashSet<HighlightSeverity>();
  private final Set<Language> mySuitableLanguages = new HashSet<Language>();
  private Boolean mySuitableInspectionsStates;
  private boolean myAvailableOnlyForAnalyze;
  private boolean myShowOnlyCleanupInspections;

  public boolean isAvailableOnlyForAnalyze() {
    return myAvailableOnlyForAnalyze;
  }

  public boolean isShowOnlyCleanupInspections() {
    return myShowOnlyCleanupInspections;
  }

  public Boolean getSuitableInspectionsStates() {
    return mySuitableInspectionsStates;
  }

  public boolean containsSeverity(HighlightSeverity severity) {
    return mySuitableSeverities.contains(severity);
  }

  public boolean containsLanguage(Language languageId) {
    return mySuitableLanguages.contains(languageId);
  }

  public void setShowOnlyCleanupInspections(boolean showOnlyCleanupInspections) {
    myShowOnlyCleanupInspections = showOnlyCleanupInspections;
    filterChanged();
  }

  public void setAvailableOnlyForAnalyze(boolean availableOnlyForAnalyze) {
    myAvailableOnlyForAnalyze = availableOnlyForAnalyze;
    filterChanged();
  }

  public void setSuitableInspectionsStates(@Nullable Boolean suitableInspectionsStates) {
    mySuitableInspectionsStates = suitableInspectionsStates;
    filterChanged();
  }

  public void addSeverity(HighlightSeverity severity) {
    mySuitableSeverities.add(severity);
    filterChanged();
  }

  public void removeSeverity(HighlightSeverity severity) {
    mySuitableSeverities.remove(severity);
    filterChanged();
  }

  public void addLanguage(@Nonnull Language language) {
    mySuitableLanguages.add(language);
    filterChanged();
  }

  public void addLanguages(@Nonnull Collection<Language> language) {
    mySuitableLanguages.addAll(language);
    filterChanged();
  }

  public void removeLanguage(@Nonnull Language language) {
    mySuitableLanguages.remove(language);
    filterChanged();
  }

  @Nonnull
  public Set<Language> getSuitableLanguages() {
    return mySuitableLanguages;
  }

  public void reset() {
    mySuitableInspectionsStates = null;
    myAvailableOnlyForAnalyze = false;
    myShowOnlyCleanupInspections = false;
    mySuitableSeverities.clear();
    mySuitableLanguages.clear();
    filterChanged();
  }

  public boolean isEmptyFilter() {
    return mySuitableInspectionsStates == null
           && !myAvailableOnlyForAnalyze
           && !myShowOnlyCleanupInspections
           && mySuitableSeverities.isEmpty()
           && mySuitableLanguages.isEmpty();
  }

  public boolean matches(Tools tools) {
    if (myShowOnlyCleanupInspections && !tools.getTool().isCleanupTool()) {
      return false;
    }

    if (mySuitableInspectionsStates != null && mySuitableInspectionsStates != tools.isEnabled()) {
      return false;
    }

    if (myAvailableOnlyForAnalyze != isAvailableOnlyForAnalyze(tools)) {
      return false;
    }

    if (!mySuitableSeverities.isEmpty()) {
      boolean suitable = false;
      for (ScopeToolState state : tools.getTools()) {
        if (mySuitableInspectionsStates != null && mySuitableInspectionsStates != state.isEnabled()) {
          continue;
        }
        if (mySuitableSeverities.contains(tools.getDefaultState().getLevel().getSeverity())) {
          suitable = true;
          break;
        }
      }
      if (!suitable) {
        return false;
      }
    }

    if(mySuitableLanguages.isEmpty()) {
      return true;
    }
    Language language = tools.getDefaultState().getTool().getLanguage();
    return language != null && mySuitableLanguages.contains(language);
  }

  protected abstract void filterChanged();

  private static boolean isAvailableOnlyForAnalyze(Tools tools) {
    InspectionToolWrapper tool = tools.getTool();
    return tool instanceof GlobalInspectionToolWrapper && ((GlobalInspectionToolWrapper)tool).worksInBatchModeOnly();
  }
}