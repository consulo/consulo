// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.find;

import com.intellij.icons.AllIcons;
import com.intellij.ide.IdeBundle;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import consulo.ui.image.Image;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class FindAllAction extends AnAction implements ShortcutProvider, DumbAware {
  public FindAllAction() {
    super(IdeBundle.message("show.in.find.window.button.name"), IdeBundle.message("show.in.find.window.button.description"), null);
  }

  @Override
  public void update(final @Nonnull AnActionEvent e) {
    Project project = e.getProject();
    Editor editor = e.getData(CommonDataKeys.EDITOR_EVEN_IF_INACTIVE);
    EditorSearchSession search = e.getData(EditorSearchSession.SESSION_KEY);

    e.getPresentation().setIcon(getIcon(project));
    e.getPresentation().setEnabled(
            editor != null && project != null && search != null && !project.isDisposed() && search.hasMatches() && PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument()) != null);
  }

  @Override
  public void actionPerformed(final @Nonnull AnActionEvent e) {
    Editor editor = e.getRequiredData(CommonDataKeys.EDITOR_EVEN_IF_INACTIVE);
    Project project = e.getRequiredData(CommonDataKeys.PROJECT);
    EditorSearchSession search = e.getRequiredData(EditorSearchSession.SESSION_KEY);
    if (project.isDisposed()) return;
    PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
    if (file == null) return;

    FindModel oldModel = FindManager.getInstance(project).getFindInFileModel();
    FindModel newModel = oldModel.clone();
    String text = search.getTextInField();
    if (StringUtil.isEmpty(text)) return;

    newModel.setStringToFind(text);
    FindUtil.findAllAndShow(project, file, newModel);
  }

  @Override
  @Nullable
  public ShortcutSet getShortcut() {
    AnAction findUsages = ActionManager.getInstance().getAction(IdeActions.ACTION_FIND_USAGES);
    return findUsages != null ? findUsages.getShortcutSet() : null;
  }

  @Nonnull
  private static Image getIcon(@Nullable Project project) {
    ToolWindowManager toolWindowManager = project != null ? ToolWindowManager.getInstance(project) : null;
    if (toolWindowManager != null) {
      return toolWindowManager.getLocationIcon(ToolWindowId.FIND, AllIcons.General.Pin_tab);
    }
    return AllIcons.General.Pin_tab;
  }
}
