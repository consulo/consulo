// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.fileEditor.impl.internal.action;

import consulo.application.ApplicationProperties;
import consulo.application.ui.wm.IdeFocusManager;
import consulo.codeEditor.Editor;
import consulo.codeEditor.LogicalPosition;
import consulo.codeEditor.ScrollType;
import consulo.fileEditor.history.IdeDocumentHistory;
import consulo.ide.localize.IdeLocalize;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.InputBoxBuilder;
import consulo.ui.InputProblem;
import consulo.ui.TextBox;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.undoRedo.CommandProcessor;
import consulo.util.lang.PatternUtil;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class GotoLineNumberDialog {
    private static final Pattern PATTERN = PatternUtil.compileSafe("\\s*(\\d+)?\\s*(?:[,:]?\\s*(\\d+)?)?\\s*", null);
    private static final Pattern OFFSET_PATTERN = PatternUtil.compileSafe("\\s*[fF]\\s*:\\s*(\\d+)\\s*", null);

    private GotoLineNumberDialog() {
    }

    @RequiredUIAccess
    public static void show(Project project, Editor editor, @Nullable Component owner, @Nullable LocalizeValue commandName) {
        LogicalPosition caret = editor.getCaretModel().getLogicalPosition();
        boolean offsetAllowed = ApplicationProperties.isInSandbox();
        LocalizeValue invalidMessage = offsetAllowed
            ? IdeLocalize.dialogMessageGoToLineInvalidPositionOrOffset()
            : IdeLocalize.dialogMessageGoToLineInvalidPosition();
        UIAccess uiAccess = UIAccess.current();

        InputBoxBuilder.text()
            .title(IdeLocalize.dialogTitleGoToLineColumn())
            .text(IdeLocalize.labelLineColumn())
            .value((caret.line + 1) + ":" + (caret.column + 1))
            .setupComponent(TextBox::selectAll)
            .validator(text -> parse(text, editor, caret.line, offsetAllowed) == null ? InputProblem.error(invalidMessage) : null)
            .showAsync(owner)
            .whenCompleteAsync((text, error) -> {
                LogicalPosition position = error == null && text != null ? parse(text, editor, caret.line, offsetAllowed) : null;
                if (position == null || editor.isDisposed() || project.isDisposed()) {
                    return;
                }

                if (commandName == null) {
                    moveCaret(editor, position);
                    return;
                }

                CommandProcessor.getInstance().newCommand()
                    .project(project)
                    .name(commandName)
                    .run(() -> {
                        moveCaret(editor, position);
                        IdeDocumentHistory.getInstance(project).includeCurrentCommandAsNavigation();
                    });
            }, uiAccess);
    }

    private static @Nullable LogicalPosition parse(String text, Editor editor, int currentLine, boolean offsetAllowed) {
        if (offsetAllowed) {
            Matcher offsetMatcher = OFFSET_PATTERN.matcher(text);
            if (offsetMatcher.matches()) {
                int offset = StringUtil.parseInt(offsetMatcher.group(1), -1);
                return offset < 0 ? null : editor.offsetToLogicalPosition(Math.min(offset, editor.getDocument().getTextLength()));
            }
        }

        Matcher m = PATTERN.matcher(text);
        if (!m.matches()) {
            return null;
        }

        int line = StringUtil.parseInt(m.group(1), currentLine + 1);
        int column = StringUtil.parseInt(m.group(2), -1);
        return line > 0 ? new LogicalPosition(line - 1, Math.max(0, column - 1)) : null;
    }

    @RequiredUIAccess
    private static void moveCaret(Editor editor, LogicalPosition position) {
        editor.getCaretModel().removeSecondaryCarets();
        editor.getCaretModel().moveToLogicalPosition(position);
        editor.getScrollingModel().scrollToCaret(ScrollType.CENTER);
        editor.getSelectionModel().removeSelection();
        IdeFocusManager.getGlobalInstance().requestFocus(editor.getContentUIComponent(), true);
    }
}
