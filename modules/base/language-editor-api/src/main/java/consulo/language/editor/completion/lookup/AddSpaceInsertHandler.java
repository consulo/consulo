package consulo.language.editor.completion.lookup;

import consulo.annotation.UsedInPlugin;
import consulo.codeEditor.Editor;
import consulo.codeEditor.util.EditorModificationUtil;
import consulo.document.Document;
import consulo.language.editor.AutoPopupController;
import consulo.language.psi.PsiDocumentManager;
import consulo.project.Project;
import consulo.util.lang.StringUtil;

/**
 * @author zolotov
 */
@UsedInPlugin
public class AddSpaceInsertHandler implements InsertHandler<LookupElement> {
  public final static InsertHandler<LookupElement> INSTANCE = new AddSpaceInsertHandler(false);
  public final static InsertHandler<LookupElement> INSTANCE_WITH_AUTO_POPUP = new AddSpaceInsertHandler(true);

  private final String myIgnoreOnChars;
  private final boolean myTriggerAutoPopup;

  public AddSpaceInsertHandler(boolean triggerAutoPopup) {
    this("", triggerAutoPopup);
  }

  public AddSpaceInsertHandler(String ignoreOnChars, boolean triggerAutoPopup) {
    myIgnoreOnChars = ignoreOnChars;
    myTriggerAutoPopup = triggerAutoPopup;
  }

  @Override
  public void handleInsert(InsertionContext context, LookupElement item) {
    Editor editor = context.getEditor();
    char completionChar = context.getCompletionChar();
    if (completionChar == ' ' || StringUtil.containsChar(myIgnoreOnChars, completionChar)) return;
    Project project = editor.getProject();
    if (project != null) {
      if (!isCharAtSpace(editor)) {
        EditorModificationUtil.insertStringAtCaret(editor, " ");
        PsiDocumentManager.getInstance(project).commitDocument(editor.getDocument());
      }
      else if (shouldOverwriteExistingSpace(editor)) {
        editor.getCaretModel().moveToOffset(editor.getCaretModel().getOffset() + 1);
      }
      if (myTriggerAutoPopup) {
        AutoPopupController.getInstance(project).autoPopupMemberLookup(editor, null);
      }
    }
  }

  protected boolean shouldOverwriteExistingSpace(Editor editor) {
    return true;
  }

  private static boolean isCharAtSpace(Editor editor) {
    final int startOffset = editor.getCaretModel().getOffset();
    final Document document = editor.getDocument();
    return document.getTextLength() > startOffset && document.getCharsSequence().charAt(startOffset) == ' ';
  }
}
