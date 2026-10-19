/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.diff.impl.internal.viewer;

import consulo.codeEditor.Editor;
import consulo.codeEditor.event.EditorMouseEvent;
import consulo.codeEditor.event.EditorMouseEventArea;
import consulo.codeEditor.event.EditorMouseListener;
import consulo.dataContext.DataManager;
import consulo.diff.impl.internal.action.OpenInEditorAction;
import consulo.navigation.Navigatable;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.details.ModifiedInputDetails;
import consulo.ui.event.details.MouseInputDetails;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.IdeActions;
import consulo.ui.ex.action.MouseShortcut;
import consulo.ui.ex.action.Shortcut;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;

public abstract class OpenInEditorWithMouseAction {
    private final Shortcut[] myShortcuts;

    public OpenInEditorWithMouseAction() {
        AnAction navigateAction = ActionManager.getInstance().getAction(IdeActions.ACTION_GOTO_DECLARATION); // null in MPS
        myShortcuts = navigateAction != null ? navigateAction.getShortcutSet().getShortcuts() : Shortcut.EMPTY_ARRAY;
    }

    public void install(List<? extends Editor> editors) {
        for (Editor editor : editors) {
            editor.addEditorMouseListener(new EditorMouseListener() {
                @Override
                @RequiredUIAccess
                public void mouseClicked(EditorMouseEvent event) {
                    if (event.getArea() != EditorMouseEventArea.LINE_NUMBERS_AREA) {
                        return;
                    }
                    if (!(event.getInputDetails() instanceof MouseInputDetails details) || !matches(details)) {
                        return;
                    }

                    OpenInEditorAction openInEditorAction =
                        DataManager.getInstance().getDataContext(editor.getContentUIComponent()).getData(OpenInEditorAction.KEY);
                    if (openInEditorAction == null) {
                        return;
                    }

                    Navigatable navigatable = getNavigatable(editor, event.getLogicalPosition().line);
                    if (navigatable == null) {
                        return;
                    }

                    event.consume();
                    openInEditorAction.openEditor(navigatable);
                }
            });
        }
    }

    private boolean matches(MouseInputDetails details) {
        if (myShortcuts.length == 0) {
            return details.getButton() == MouseInputDetails.MouseButton.LEFT
                && details.getModifiers().equals(EnumSet.of(ModifiedInputDetails.Modifier.CTRL));
        }
        for (Shortcut shortcut : myShortcuts) {
            if (shortcut instanceof MouseShortcut mouseShortcut && mouseShortcut.matches(details, 1)) {
                return true;
            }
        }
        return false;
    }

    protected abstract @Nullable Navigatable getNavigatable(Editor editor, int line);
}
