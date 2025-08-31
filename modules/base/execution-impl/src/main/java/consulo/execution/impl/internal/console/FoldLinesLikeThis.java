package consulo.execution.impl.internal.console;

import consulo.codeEditor.Editor;
import consulo.codeEditor.SelectionModel;
import consulo.configurable.internal.ShowConfigurableService;
import consulo.document.Document;
import consulo.execution.ui.console.ConsoleView;
import consulo.project.Project;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

/**
 * @author peter
 */
public class FoldLinesLikeThis extends DumbAwareAction {
    private final Provider<ShowConfigurableService> myShowConfigurableService;

    @Inject
    public FoldLinesLikeThis(Provider<ShowConfigurableService> showConfigurableService) {
        myShowConfigurableService = showConfigurableService;
    }

    @Nullable
    private static String getSingleLineSelection(@Nonnull Editor editor) {
        SelectionModel model = editor.getSelectionModel();
        Document document = editor.getDocument();
        if (!model.hasSelection()) {
            int offset = editor.getCaretModel().getOffset();
            if (offset <= document.getTextLength()) {
                int lineNumber = document.getLineNumber(offset);
                String line = document.getText().substring(document.getLineStartOffset(lineNumber), document.getLineEndOffset(lineNumber)).trim();
                if (StringUtil.isNotEmpty(line)) {
                    return line;
                }
            }

            return null;
        }
        int start = model.getSelectionStart();
        int end = model.getSelectionEnd();
        if (document.getLineNumber(start) == document.getLineNumber(end)) {
            String selection = document.getText().substring(start, end).trim();
            if (StringUtil.isNotEmpty(selection)) {
                return selection;
            }
        }
        return null;
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        Editor editor = e.getData(Editor.KEY);
        e.getPresentation().setEnabledAndVisible(e.hasData(ConsoleView.KEY) && editor != null && getSingleLineSelection(editor) != null);
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        Editor editor = e.getRequiredData(Editor.KEY);
        String selection = getSingleLineSelection(editor);
        assert selection != null;

        ConsoleView consoleView = e.getData(ConsoleView.KEY);

        UIAccess uiAccess = UIAccess.current();

        myShowConfigurableService.get().showAndSelect(e.getData(Project.KEY), ConsoleConfigurable.class, consoleConfigurable -> {
            consoleConfigurable.addRule(selection);
        }).whenCompleteAsync((o, throwable) -> {
            if (consoleView != null) {
                consoleView.foldImmediately();
            }
        }, uiAccess);
    }
}
