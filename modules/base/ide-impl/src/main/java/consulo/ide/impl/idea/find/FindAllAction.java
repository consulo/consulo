// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.find;

import consulo.application.AllIcons;
import consulo.codeEditor.EditorKeys;
import consulo.codeEditor.Editor;
import consulo.application.dumb.DumbAware;
import consulo.find.FindManager;
import consulo.find.FindModel;
import consulo.platform.base.localize.IdeLocalize;
import consulo.project.Project;
import consulo.util.lang.StringUtil;
import consulo.project.ui.wm.ToolWindowId;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.ui.ex.action.*;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public final class FindAllAction extends AnAction implements ShortcutProvider, DumbAware {
  public FindAllAction() {
    super(IdeLocalize.showInFindWindowButtonName(), IdeLocalize.showInFindWindowButtonDescription(), null);
  }

  @Override
  public void update(final @Nonnull AnActionEvent e) {
    Project project = e.getData(Project.KEY);
    Editor editor = e.getData(EditorKeys.EDITOR_EVEN_IF_INACTIVE);
    EditorSearchSession search = e.getData(EditorSearchSession.SESSION_KEY);

    e.getPresentation().setIcon(getIcon(project));
    e.getPresentation().setEnabled(
            editor != null && project != null && search != null && !project.isDisposed() && search.hasMatches() && PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument()) != null);
  }

  @Override
  public void actionPerformed(final @Nonnull AnActionEvent e) {
    Editor editor = e.getRequiredData(EditorKeys.EDITOR_EVEN_IF_INACTIVE);
    Project project = e.getRequiredData(Project.KEY);
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
