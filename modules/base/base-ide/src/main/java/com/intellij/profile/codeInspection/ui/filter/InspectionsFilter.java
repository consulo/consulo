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
package com.intellij.profile.codeInspection.ui.filter;

import com.intellij.codeInspection.ex.GlobalInspectionToolWrapper;
import com.intellij.codeInspection.ex.InspectionToolWrapper;
import com.intellij.codeInspection.ex.ScopeToolState;
import com.intellij.codeInspection.ex.Tools;
import com.intellij.lang.Language;
import com.intellij.lang.annotation.HighlightSeverity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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

  public boolean containsSeverity(final HighlightSeverity severity) {
    return mySuitableSeverities.contains(severity);
  }

  public boolean containsLanguage(final Language languageId) {
    return mySuitableLanguages.contains(languageId);
  }

  public void setShowOnlyCleanupInspections(final boolean showOnlyCleanupInspections) {
    myShowOnlyCleanupInspections = showOnlyCleanupInspections;
    filterChanged();
  }

  public void setAvailableOnlyForAnalyze(final boolean availableOnlyForAnalyze) {
    myAvailableOnlyForAnalyze = availableOnlyForAnalyze;
    filterChanged();
  }

  public void setSuitableInspectionsStates(@Nullable final Boolean suitableInspectionsStates) {
    mySuitableInspectionsStates = suitableInspectionsStates;
    filterChanged();
  }

  public void addSeverity(final HighlightSeverity severity) {
    mySuitableSeverities.add(severity);
    filterChanged();
  }

  public void removeSeverity(final HighlightSeverity severity) {
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

  public boolean matches(final Tools tools) {
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
      for (final ScopeToolState state : tools.getTools()) {
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
    final String languageId = tools.getDefaultState().getTool().getLanguage();
    Language language  = languageId == null ? null : Language.findLanguageByID(languageId);
    return language != null && mySuitableLanguages.contains(language);
  }

  protected abstract void filterChanged();

  private static boolean isAvailableOnlyForAnalyze(final Tools tools) {
    final InspectionToolWrapper tool = tools.getTool();
    return tool instanceof GlobalInspectionToolWrapper && ((GlobalInspectionToolWrapper)tool).worksInBatchModeOnly();
  }
}