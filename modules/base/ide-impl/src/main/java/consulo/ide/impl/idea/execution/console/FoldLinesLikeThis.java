package consulo.ide.impl.idea.execution.console;

import consulo.application.ApplicationManager;
import consulo.application.impl.internal.IdeaModalityState;
import consulo.codeEditor.Editor;
import consulo.codeEditor.SelectionModel;
import consulo.document.Document;
import consulo.execution.ui.console.ConsoleView;
import consulo.ide.impl.idea.execution.impl.ConsoleViewImpl;
import consulo.util.lang.StringUtil;
import consulo.ide.setting.ShowSettingsUtil;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author peter
 */
public class FoldLinesLikeThis extends DumbAwareAction {

  @Nullable
  private static String getSingleLineSelection(@Nonnull Editor editor) {
    final SelectionModel model = editor.getSelectionModel();
    final Document document = editor.getDocument();
    if (!model.hasSelection()) {
      final int offset = editor.getCaretModel().getOffset();
      if (offset <= document.getTextLength()) {
        final int lineNumber = document.getLineNumber(offset);
        final String line = document.getText().substring(document.getLineStartOffset(lineNumber), document.getLineEndOffset(lineNumber)).trim();
        if (StringUtil.isNotEmpty(line)) {
          return line;
        }
      }

      return null;
    }
    final int start = model.getSelectionStart();
    final int end = model.getSelectionEnd();
    if (document.getLineNumber(start) == document.getLineNumber(end)) {
      final String selection = document.getText().substring(start, end).trim();
      if (StringUtil.isNotEmpty(selection)) {
        return selection;
      }
    }
    return null;
  }

  @Override
  public void update(AnActionEvent e) {
    final Editor editor = e.getData(Editor.KEY);

    final boolean enabled = e.getData(ConsoleView.KEY) != null && editor != null && getSingleLineSelection(editor) != null;
    e.getPresentation().setEnabled(enabled);
    e.getPresentation().setVisible(enabled);
  }

  @Override
  public void actionPerformed(AnActionEvent e) {
    final Editor editor = e.getData(Editor.KEY);
    assert editor != null;
    final String selection = getSingleLineSelection(editor);
    assert selection != null;
    ShowSettingsUtil.getInstance().editConfigurable(editor.getProject(), new ConsoleFoldingConfigurable(ConsoleFoldingSettings::getSettings) {
      @Override
      public void reset() {
        super.reset();
        ApplicationManager.getApplication().invokeLater(() -> addRule(selection), IdeaModalityState.stateForComponent(createComponent()));
      }
    });
    final ConsoleView consoleView = e.getData(ConsoleView.KEY);
    if (consoleView instanceof ConsoleViewImpl) {
      ((ConsoleViewImpl)consoleView).foldImmediately();
    }
  }
}
