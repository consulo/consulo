/*
 * Copyright 2013-2026 consulo.io
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
package consulo.desktop.qt.editor.impl.internal;

import consulo.ide.impl.idea.codeInsight.navigation.actions.GotoDeclarationAction;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;

/**
 * The go to declaration click for the qt editor - ctrl/cmd click and middle click. The hover half
 * (underline and hand cursor) is the platform ctrl hover handling, driven by the editor mouse events
 * the widget fires.
 */
public class DesktopQtEditorLinkNavigation {
    private final DesktopQtEditorImpl myEditor;

    public DesktopQtEditorLinkNavigation(DesktopQtEditorImpl editor) {
        myEditor = editor;
    }

    /**
     * Go to declaration, which ctrl click is bound to. The target is resolved the same way the awt action does,
     * since a raw element is not the declaration and is not navigable by itself either.
     */
    @RequiredUIAccess
    public void navigateTo(int offset) {
        Project project = myEditor.getProject();
        if (project == null) {
            return;
        }

        myEditor.getCaretModel().moveToOffset(offset);

        GotoDeclarationAction.navigateToDeclaration(project, myEditor, offset);
    }
}
