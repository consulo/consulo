package com.intellij.execution.console;

import com.intellij.execution.impl.ConsoleViewImpl;
import consulo.execution.ui.console.ConsoleView;
import consulo.ui.ex.action.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import consulo.application.ApplicationManager;
import consulo.application.impl.internal.IdeaModalityState;
import consulo.document.Document;
import consulo.codeEditor.Editor;
import consulo.codeEditor.SelectionModel;
import consulo.ide.setting.ShowSettingsUtil;
import consulo.ui.ex.action.DumbAwareAction;
import com.intellij.openapi.util.text.StringUtil;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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
    final Editor editor = e.getData(PlatformDataKeys.EDITOR);

    final boolean enabled = e.getData(LangDataKeys.CONSOLE_VIEW) != null &&  editor != null && getSingleLineSelection(editor) != null;
    e.getPresentation().setEnabled(enabled);
    e.getPresentation().setVisible(enabled);
  }

  @Override
  public void actionPerformed(AnActionEvent e) {
    final Editor editor = e.getData(PlatformDataKeys.EDITOR);
    assert editor != null;
    final String selection = getSingleLineSelection(editor);
    assert selection != null;
    ShowSettingsUtil.getInstance().editConfigurable(editor.getProject(), new ConsoleFoldingConfigurable() {
      @Override
      public void reset() {
        super.reset();
        ApplicationManager.getApplication().invokeLater(new Runnable() {
          @Override
          public void run() {
            addRule(selection);
          }
        }, IdeaModalityState.stateForComponent(createComponent()));
      }
    });
    final ConsoleView consoleView = e.getData(LangDataKeys.CONSOLE_VIEW);
    if (consoleView instanceof ConsoleViewImpl) {
      ((ConsoleViewImpl)consoleView).foldImmediately();
    }
  }
}
