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
package consulo.codeEditor.impl.internal.floating;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorKind;
import consulo.codeEditor.PersistentEditorSettings;
import consulo.codeEditor.event.EditorFactoryEvent;
import consulo.codeEditor.event.EditorFactoryListener;
import consulo.codeEditor.internal.EditorSelectionGestureSupport;
import consulo.codeEditor.toolbar.floating.CodeFloatingToolbar;
import consulo.disposer.Disposer;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionManager;
import jakarta.inject.Inject;

/**
 * @author VISTALL
 * @since 2026-11-09
 */
@ExtensionImpl
public class CodeFloatingToolbarInstaller implements EditorFactoryListener {
    private final Application myApplication;
    private final ActionManager myActionManager;
    private final PersistentEditorSettings myEditorSettings;

    @Inject
    public CodeFloatingToolbarInstaller(Application application, ActionManager actionManager, PersistentEditorSettings editorSettings) {
        myApplication = application;
        myActionManager = actionManager;
        myEditorSettings = editorSettings;
    }

    @Override
    @RequiredUIAccess
    public void editorCreated(EditorFactoryEvent event) {
        Editor editor = event.getEditor();
        Project project = editor.getProject();
        if (project == null
            || editor.getEditorKind() != EditorKind.MAIN_EDITOR
            || !(editor instanceof EditorSelectionGestureSupport)
            || !EditorFloatingToolbarInstaller.mayShowToolbar(editor)) {
            return;
        }

        CodeFloatingToolbarImpl toolbar = new CodeFloatingToolbarImpl(myApplication, editor, project, myActionManager, myEditorSettings);
        editor.putUserData(CodeFloatingToolbar.KEY, toolbar);
    }

    @Override
    public void editorReleased(EditorFactoryEvent event) {
        Editor editor = event.getEditor();
        CodeFloatingToolbar toolbar = editor.getUserData(CodeFloatingToolbar.KEY);
        if (toolbar instanceof CodeFloatingToolbarImpl impl) {
            editor.putUserData(CodeFloatingToolbar.KEY, null);
            Disposer.dispose(impl);
        }
    }
}
