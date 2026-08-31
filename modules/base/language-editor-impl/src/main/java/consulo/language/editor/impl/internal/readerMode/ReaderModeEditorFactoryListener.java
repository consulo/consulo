// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor.impl.internal.readerMode;

import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Editor;
import consulo.codeEditor.RealEditor;
import consulo.codeEditor.event.EditorFactoryEvent;
import consulo.codeEditor.event.EditorFactoryListener;
import consulo.document.FileDocumentManager;
import consulo.language.editor.readerMode.ReaderModeSettings;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.virtualFileSystem.VirtualFile;

@ExtensionImpl
public class ReaderModeEditorFactoryListener implements EditorFactoryListener {
    @RequiredUIAccess
    @Override
    public void editorCreated(EditorFactoryEvent event) {
        Editor editor = event.getEditor();
        Project project = editor.getProject();
        if (project == null || !project.isInitialized() || project.isDefault()
            || !ReaderModeSettings.getInstance(project).isEnabled()) {
            return;
        }
        if (!(editor instanceof RealEditor)) {
            return;
        }

        VirtualFile file = FileDocumentManager.getInstance().getFile(editor.getDocument());
        if (file == null) {
            return;
        }
        ReaderModeSettings.applyReaderMode(project, editor, file);
    }
}
