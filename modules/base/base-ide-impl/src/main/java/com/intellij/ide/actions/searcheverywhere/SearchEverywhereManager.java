// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.searcheverywhere;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import consulo.util.dataholder.Key;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author Mikhail.Sokolov
 */
public interface SearchEverywhereManager {
  public static final Key<JBPopup> SEARCH_EVERYWHERE_POPUP = new Key<>("SearchEverywherePopup");

  static SearchEverywhereManager getInstance(Project project) {
    return ServiceManager.getService(project, SearchEverywhereManager.class);
  }

  boolean isShown();

  void show(@Nonnull String contributorID, @Nullable String searchText, @Nonnull AnActionEvent initEvent); //todo change to contributor??? UX-1

  @Nonnull
  String getSelectedContributorID();

  void setSelectedContributor(@Nonnull String contributorID); //todo change to contributor??? UX-1

  void toggleEverywhereFilter();

  // todo remove
  boolean isEverywhere();

}
