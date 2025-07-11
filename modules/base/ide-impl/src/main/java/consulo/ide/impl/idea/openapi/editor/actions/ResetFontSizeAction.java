/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

import consulo.execution.ui.console.ConsoleViewUtil;
import consulo.dataContext.DataContext;
import consulo.codeEditor.Editor;
import consulo.codeEditor.action.EditorAction;
import consulo.codeEditor.action.EditorActionHandler;
import consulo.colorScheme.EditorColorsManager;
import consulo.colorScheme.EditorColorsScheme;
import consulo.codeEditor.EditorEx;
import jakarta.annotation.Nonnull;

/**
 * @author Denis Zhdanov
 */
public class ResetFontSizeAction extends EditorAction {
    public ResetFontSizeAction() {
        super(new MyHandler());
    }

    private static class MyHandler extends EditorActionHandler {
        @Override
        public void execute(@Nonnull Editor editor, DataContext dataContext) {
            if (!(editor instanceof EditorEx editorEx)) {
                return;
            }
            EditorColorsScheme globalScheme = EditorColorsManager.getInstance().getGlobalScheme();
            int fontSize =
                ConsoleViewUtil.isConsoleViewEditor(editor) ? globalScheme.getConsoleFontSize() : globalScheme.getEditorFontSize();
            editorEx.setFontSize(fontSize);
        }
    }
}
