// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.util;

import consulo.annotation.access.RequiredReadAction;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorPopupHelper;
import consulo.fileEditor.FileEditorManager;
import consulo.language.editor.internal.LanguageEditorInternalHelper;
import consulo.language.impl.psi.PomTargetPsiElementImpl;
import consulo.language.pom.PomRenameableTarget;
import consulo.language.pom.PomTargetPsiElement;
import consulo.language.psi.PsiElement;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.ui.RelativePoint2D;
import consulo.ui.annotation.RequiredUIAccess;

import java.util.function.Consumer;

public class CommonFakeNavigatablePomTarget extends PomTargetPsiElementImpl {
    public CommonFakeNavigatablePomTarget(Project project, PomRenameableTarget<?> pomTarget) {
        super(project, pomTarget);
    }

    @Override
    @RequiredReadAction
    public void navigate(boolean requestFocus) {
        showFindUsages();
    }

    @RequiredUIAccess
    protected void showFindUsages() {
        Consumer<CommonFakeNavigatablePomTarget> mockedFindUsages = CommonFakeNavigatablePomTargetUtil.getMockedFindUsages();
        if (mockedFindUsages != null) {
            mockedFindUsages.accept(this);
            return;
        }
        Project project = getProject();
        if (DumbService.getInstance(project).isDumb()) {
            return;
        }
        Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
        if (editor == null) {
            return;
        }
        RelativePoint2D popupPosition = EditorPopupHelper.getInstance().guessBestPopupLocation(editor);
        LanguageEditorInternalHelper.getInstance().startFindUsages(editor, project, this, popupPosition);
    }

    @Override
    @RequiredReadAction
    public boolean canNavigate() {
        return true;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + getTarget() + ")";
    }

    @Override
    public boolean isEquivalentTo(PsiElement another) {
        return super.isEquivalentTo(another) || (another instanceof PomTargetPsiElement pomTargetPsiElement
            && pomTargetPsiElement.getTarget().equals(getTarget()));
    }
}
