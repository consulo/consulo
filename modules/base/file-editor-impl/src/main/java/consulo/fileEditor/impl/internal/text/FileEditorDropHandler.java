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
package consulo.fileEditor.impl.internal.text;

import consulo.codeEditor.Editor;
import consulo.codeEditor.impl.EditorDropHandler;
import consulo.fileEditor.FileEditorWindow;
import consulo.fileEditor.impl.internal.FileDropManager;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.dnd.FileCopyPasteUtil;
import org.jspecify.annotations.Nullable;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.util.List;

public class FileEditorDropHandler implements EditorDropHandler {
    private final @Nullable Editor myEditor;

    public FileEditorDropHandler(@Nullable Editor editor) {
        myEditor = editor;
    }

    @Override
    public boolean canHandleDrop(DataFlavor[] transferFlavors) {
        return transferFlavors != null && FileCopyPasteUtil.isFileListFlavorAvailable(transferFlavors);
    }

    @Override
    @RequiredUIAccess
    public void handleDrop(Transferable t, @Nullable Project project, Object editorWindow) {
        if (project == null) {
            return;
        }

        List<File> fileList = FileCopyPasteUtil.getFileList(t);
        if (fileList != null) {
            FileDropManager.getInstance(project).scheduleDrop(fileList, myEditor, (FileEditorWindow) editorWindow);
        }
    }
}
