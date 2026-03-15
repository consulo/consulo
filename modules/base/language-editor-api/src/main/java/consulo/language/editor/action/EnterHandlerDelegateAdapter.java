package consulo.language.editor.action;

import consulo.codeEditor.Editor;
import consulo.codeEditor.action.EditorActionHandler;
import consulo.dataContext.DataContext;
import consulo.language.psi.PsiFile;
import consulo.util.lang.ref.SimpleReference;

/**
 * @author Denis Zhdanov
 * @since 2011-05-30
 */
public class EnterHandlerDelegateAdapter implements EnterHandlerDelegate {
    @Override
    public Result preprocessEnter(
        PsiFile file,
        Editor editor,
        SimpleReference<Integer> caretOffset,
        SimpleReference<Integer> caretAdvance,
        DataContext dataContext,
        EditorActionHandler originalHandler
    ) {
        return Result.Continue;
    }

    @Override
    public Result postProcessEnter(PsiFile file, Editor editor, DataContext dataContext) {
        return Result.Continue;
    }
}
