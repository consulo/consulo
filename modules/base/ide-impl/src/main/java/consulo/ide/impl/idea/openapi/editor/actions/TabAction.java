/*
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.ide.impl.idea.openapi.editor.actions;

import consulo.annotation.access.RequiredWriteAction;
import consulo.codeEditor.Caret;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorEx;
import consulo.codeEditor.action.EditorAction;
import consulo.codeEditor.action.EditorActionManager;
import consulo.codeEditor.action.EditorActionUtil;
import consulo.codeEditor.action.EditorWriteActionHandler;
import consulo.codeEditor.localize.CodeEditorLocalize;
import consulo.dataContext.DataContext;
import consulo.document.Document;
import consulo.document.ReadOnlyFragmentModificationException;
import consulo.ide.impl.idea.openapi.editor.EditorModificationUtil;
import consulo.ide.impl.idea.openapi.editor.ex.util.EditorUIUtil;
import consulo.language.codeStyle.CodeStyleSettings;
import consulo.language.codeStyle.CodeStyleSettingsManager;
import consulo.language.codeStyle.CommonCodeStyleSettings;
import consulo.project.Project;
import consulo.undoRedo.CommandProcessor;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/*
 * Created by IntelliJ IDEA.
 * User: max
 * Date: May 13, 2002
 * Time: 9:51:34 PM
 * To change template for new class use
 * Code Style | Class Templates options (Tools | IDE Options).
 */
public class TabAction extends EditorAction {
    public TabAction() {
        super(new Handler());
        setInjectedContext(true);
    }

    private static class Handler extends EditorWriteActionHandler {
        public Handler() {
            super(true);
        }

        @RequiredWriteAction
        @Override
        public void executeWriteAction(Editor editor, @Nullable Caret caret, DataContext dataContext) {
            if (caret == null) {
                caret = editor.getCaretModel().getPrimaryCaret();
            }
            CommandProcessor.getInstance().setCurrentCommandGroupId(EditorActionUtil.EDIT_COMMAND_GROUP);
            CommandProcessor.getInstance().setCurrentCommandName(CodeEditorLocalize.typingCommandName());
            Project project = dataContext.getData(Project.KEY);
            insertTabAtCaret(editor, caret, project);
        }

        @Override
        public boolean isEnabled(Editor editor, DataContext dataContext) {
            return !editor.isOneLineMode() && !((EditorEx) editor).isEmbeddedIntoDialogWrapper() && !editor.isViewer();
        }
    }

    private static void insertTabAtCaret(Editor editor, @Nonnull Caret caret, @Nullable Project project) {
        EditorUIUtil.hideCursorInEditor(editor);
        int columnNumber;
        if (caret.hasSelection()) {
            columnNumber = editor.visualToLogicalPosition(caret.getSelectionStartPosition()).column;
        }
        else {
            columnNumber = editor.getCaretModel().getLogicalPosition().column;
        }

        CodeStyleSettings settings = CodeStyleSettingsManager.getSettings(project);

        final Document doc = editor.getDocument();
        CommonCodeStyleSettings.IndentOptions indentOptions = settings.getIndentOptionsByDocument(project, doc);

        int tabSize = indentOptions.INDENT_SIZE;
        int spacesToAddCount = tabSize - columnNumber % Math.max(1, tabSize);

        boolean useTab = editor.getSettings().isUseTabCharacter(project);

        CharSequence chars = doc.getCharsSequence();
        if (useTab && indentOptions.SMART_TABS) {
            int offset = editor.getCaretModel().getOffset();
            while (offset > 0) {
                offset--;
                if (chars.charAt(offset) == '\t') {
                    continue;
                }
                if (chars.charAt(offset) == '\n') {
                    break;
                }
                useTab = false;
                break;
            }
        }

        doc.startGuardedBlockChecking();
        try {
            EditorModificationUtil.insertStringAtCaret(editor, useTab ? "\t" : StringUtil.repeatSymbol(' ', spacesToAddCount), false, true);
        }
        catch (ReadOnlyFragmentModificationException e) {
            EditorActionManager.getInstance().getReadonlyFragmentModificationHandler(doc).handle(e);
        }
        finally {
            doc.stopGuardedBlockChecking();
        }
    }
}
