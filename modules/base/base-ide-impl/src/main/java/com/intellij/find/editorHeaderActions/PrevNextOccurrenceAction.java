// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.find.editorHeaderActions;

import com.intellij.find.SearchSession;
import consulo.ui.ex.action.AnActionEvent;
import consulo.dataContext.DataContext;
import consulo.ui.ex.action.Shortcut;
import consulo.ui.ex.action.ShortcutSet;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.util.containers.ContainerUtil;
import javax.annotation.Nonnull;

import java.util.List;

public abstract class PrevNextOccurrenceAction extends DumbAwareAction implements ContextAwareShortcutProvider {
  protected final boolean mySearch;

  PrevNextOccurrenceAction(@Nonnull String templateActionId, boolean search) {
    mySearch = search;
    ActionUtil.copyFrom(this, templateActionId);
  }

  @Override
  public final void update(@Nonnull AnActionEvent e) {
    SearchSession search = e.getData(SearchSession.KEY);
    e.getPresentation().setEnabled(search != null && !search.isSearchInProgress() && search.hasMatches());
  }

  @Override
  public final ShortcutSet getShortcut(@Nonnull DataContext context) {
    SearchSession search = context.getData(SearchSession.KEY);
    boolean singleLine = search != null && !search.getFindModel().isMultiline();
    return Utils.shortcutSetOf(singleLine ? ContainerUtil.concat(getDefaultShortcuts(), getSingleLineShortcuts()) : getDefaultShortcuts());
  }

  @Nonnull
  protected abstract List<Shortcut> getDefaultShortcuts();

  @Nonnull
  protected abstract List<Shortcut> getSingleLineShortcuts();
}
