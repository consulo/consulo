// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.actions;

import consulo.annotation.access.RequiredReadAction;
import consulo.codeEditor.Editor;
import consulo.dataContext.DataContext;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.ui.ex.awt.dnd.FileCopyPasteUtil;
import consulo.ide.localize.IdeLocalize;
import consulo.language.editor.QualifiedNameProviderUtil;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiFileSystemItem;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.awt.CopyPasteManager;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.util.Collections;
import java.util.List;

import static consulo.ide.impl.idea.ide.actions.CopyReferenceUtil.*;

/**
 * @author Alexey
 */
public class CopyReferenceAction extends DumbAwareAction {
    public static final DataFlavor ourFlavor = FileCopyPasteUtil.createJvmDataFlavor(CopyReferenceFQNTransferable.class);

    public CopyReferenceAction() {
        super();
        setEnabledInModalContext(true);
        setInjectedContext(true);
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        boolean plural = false;
        boolean enabled;
        boolean paths = false;
        boolean calcQualifiedName = ActionPlaces.COPY_REFERENCE_POPUP.equals(e.getPlace());

        Editor editor = e.getData(Editor.KEY);
        if (editor != null && FileDocumentManager.getInstance().getFile(editor.getDocument()) != null) {
            enabled = true;
        }
        else {
            List<PsiElement> elements = getPsiElements(e.getDataContext(), editor);
            enabled = !elements.isEmpty();
            plural = elements.size() > 1;
            paths = elements.stream().allMatch(el -> el instanceof PsiFileSystemItem && getQualifiedNameFromProviders(el) == null);

            if (calcQualifiedName) {
                e.getPresentation().putClientProperty(CopyPathProvider.QUALIFIED_NAME, getQualifiedName(editor, elements));
            }
        }

        e.getPresentation().setEnabled(enabled);
        if (ActionPlaces.isPopupPlace(e.getPlace())) {
            e.getPresentation().setVisible(enabled);
        }
        else {
            e.getPresentation().setVisible(true);
        }
        e.getPresentation().setTextValue(
            paths
                ? plural
                ? IdeLocalize.copyRelativePaths()
                : IdeLocalize.copyRelativePath()
                : plural
                ? IdeLocalize.copyReferences()
                : IdeLocalize.copyReference()
        );

        if (paths) {
            e.getPresentation().setEnabledAndVisible(false);
        }
    }

    @Nonnull
    @RequiredReadAction
    protected List<PsiElement> getPsiElements(DataContext dataContext, Editor editor) {
        return getElementsToCopy(editor, dataContext);
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        Editor editor = e.getData(Editor.KEY);
        Project project = e.getData(Project.KEY);
        List<PsiElement> elements = getPsiElements(e.getDataContext(), editor);

        String copy = getQualifiedName(editor, elements);
        if (copy != null) {
            CopyPasteManager.getInstance().setContents(new CopyReferenceFQNTransferable(copy));
        }
        else if (editor != null && project != null) {
            Document document = editor.getDocument();
            PsiFile file = PsiDocumentManager.getInstance(project).getCachedPsiFile(document);
            if (file != null) {
                String toCopy = QualifiedNameProviderUtil.getFileFqn(file) + ":" + (editor.getCaretModel().getLogicalPosition().line + 1);
                CopyPasteManager.getInstance().setContents(new StringSelection(toCopy));
            }
            return;
        }

        highlight(editor, project, elements);
    }

    protected String getQualifiedName(Editor editor, List<? extends PsiElement> elements) {
        return CopyReferenceUtil.doCopy(elements, editor);
    }

    public static boolean doCopy(PsiElement element, Project project) {
        return doCopy(Collections.singletonList(element), project);
    }

    private static boolean doCopy(List<? extends PsiElement> elements, @Nullable Project project) {
        String toCopy = CopyReferenceUtil.doCopy(elements, null);
        CopyPasteManager.getInstance().setContents(new CopyReferenceFQNTransferable(toCopy));
        return true;
    }

    @Nullable
    public static String elementToFqn(@Nullable PsiElement element) {
        return CopyReferenceUtil.elementToFqn(element, null);
    }
}
