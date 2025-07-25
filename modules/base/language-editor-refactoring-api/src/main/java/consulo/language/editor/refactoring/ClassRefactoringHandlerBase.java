package consulo.language.editor.refactoring;

import consulo.annotation.UsedInPlugin;
import consulo.codeEditor.Editor;
import consulo.codeEditor.ScrollType;
import consulo.dataContext.DataContext;
import consulo.language.editor.refactoring.action.RefactoringActionHandler;
import consulo.language.editor.refactoring.localize.RefactoringLocalize;
import consulo.language.editor.refactoring.util.CommonRefactoringUtil;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.util.PsiNavigateUtil;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.annotation.Nonnull;

/**
 * @author Dennis.Ushakov
 */
@UsedInPlugin
public abstract class ClassRefactoringHandlerBase implements RefactoringActionHandler, ElementsHandler {
    @Override
    public boolean isEnabledOnElements(PsiElement[] elements) {
        return elements.length == 1 && acceptsElement(elements[0]);
    }

    protected static void navigate(PsiElement element) {
        PsiNavigateUtil.navigate(element);
    }

    @Override
    @RequiredUIAccess
    public void invoke(@Nonnull Project project, Editor editor, PsiFile file, DataContext dataContext) {
        int offset = editor.getCaretModel().getOffset();
        editor.getScrollingModel().scrollToCaret(ScrollType.MAKE_VISIBLE);
        PsiElement position = file.findElementAt(offset);
        PsiElement element = position;

        while (true) {
            if (element == null || element instanceof PsiFile) {
                LocalizeValue message = RefactoringLocalize.cannotPerformRefactoringWithReason(getInvalidPositionMessage());
                CommonRefactoringUtil.showErrorHint(project, editor, message.get(), getTitle(), getHelpId());
                return;
            }

            if (!CommonRefactoringUtil.checkReadOnlyStatus(project, element)) {
                return;
            }

            if (acceptsElement(element)) {
                invoke(project, new PsiElement[]{position}, dataContext);
                return;
            }
            element = element.getParent();
        }
    }

    @Override
    @RequiredUIAccess
    public void invoke(@Nonnull Project project, @Nonnull PsiElement[] elements, DataContext dataContext) {
        PsiFile file = dataContext.getData(PsiFile.KEY);
        Editor editor = dataContext.getData(Editor.KEY);
        showDialog(project, elements[0], editor, file, dataContext);
    }

    protected abstract boolean acceptsElement(PsiElement element);

    protected abstract void showDialog(Project project, PsiElement element, Editor editor, PsiFile file, DataContext dataContext);

    protected abstract String getHelpId();

    protected abstract String getTitle();

    protected abstract String getInvalidPositionMessage();
}
