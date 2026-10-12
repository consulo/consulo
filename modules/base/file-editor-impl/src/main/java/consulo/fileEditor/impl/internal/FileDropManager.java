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
package consulo.fileEditor.impl.internal;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.codeEditor.Editor;
import consulo.codeEditor.FileDropEvent;
import consulo.codeEditor.FileDropHandler;
import consulo.document.FileDocumentManager;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.FileEditorManager;
import consulo.fileEditor.FileEditorWindow;
import consulo.fileEditor.FileEditorWithProviderComposite;
import consulo.fileEditor.TextEditor;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.UIAction;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.clipboard.DataTransfer;
import consulo.ui.clipboard.DataTransferType;
import consulo.util.concurrent.coroutine.Continuation;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.CoroutineScope;
import consulo.util.concurrent.coroutine.step.CodeExecution;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Opens files dropped onto an editor or the editor area, unless a {@link FileDropHandler} takes the drop first.
 */
@Singleton
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
public class FileDropManager {
    private static final Logger LOG = Logger.getInstance(FileDropManager.class);

    private record Drop(@Nullable FileEditorWindow window, List<File> files) {
    }

    private record Resolved(@Nullable FileEditorWindow window, List<VirtualFile> files) {
    }

    public static FileDropManager getInstance(Project project) {
        return project.getInstance(FileDropManager.class);
    }

    private final Project myProject;

    @Inject
    public FileDropManager(Project project) {
        myProject = project;
    }

    @RequiredUIAccess
    public boolean drop(DataTransfer transfer, @Nullable FileEditorWindow editorWindowCandidate, boolean checkOnly) {
        List<File> files = transfer.get(DataTransferType.FILE_LIST);
        if (files == null || files.isEmpty()) {
            return false;
        }

        if (!checkOnly) {
            scheduleDrop(files, null, editorWindowCandidate);
        }
        return true;
    }

    public void scheduleDrop(List<File> files, @Nullable Editor editor, @Nullable FileEditorWindow editorWindowCandidate) {
        if (files.isEmpty()) {
            return;
        }

        FileDropEvent event = new FileDropEvent(myProject, List.copyOf(files), editor);

        CoroutineScope.launchAsync(
            myProject.coroutineContext(),
            () -> Coroutine
                .first(UIAction.<Object, Drop>apply((ignored, continuation) -> handle(event, editorWindowCandidate, continuation)))
                .then(CodeExecution.<Drop, Resolved>apply(FileDropManager::resolve))
                .then(UIAction.<Resolved, Resolved>apply(resolved -> {
                    open(resolved);
                    return resolved;
                }))
        );
    }

    @RequiredUIAccess
    @SuppressWarnings("unchecked")
    private @Nullable Drop handle(FileDropEvent event, @Nullable FileEditorWindow editorWindowCandidate, Continuation<?> continuation) {
        boolean handled = myProject.getExtensionPoint(FileDropHandler.class).anyMatchSafe(handler -> {
            try {
                return handler.handleDrop(event);
            }
            catch (Exception e) {
                LOG.error("Unable to handle drop event in " + handler, e);
                return false;
            }
        });

        if (handled) {
            ((Continuation<Resolved>) continuation).finishEarly(null);
            return null;
        }

        FileEditorWindow window = editorWindowCandidate != null ? editorWindowCandidate : findEditorWindow(event.editor());
        return new Drop(window, event.files());
    }

    private static Resolved resolve(Drop drop) {
        LocalFileSystem fileSystem = LocalFileSystem.getInstance();

        List<VirtualFile> files = new ArrayList<>(drop.files().size());
        for (File file : drop.files()) {
            VirtualFile virtualFile = fileSystem.refreshAndFindFileByIoFile(file);
            if (virtualFile != null) {
                files.add(virtualFile);
            }
        }

        NonProjectFileWritingAccessProvider.allowWriting(files);
        return new Resolved(drop.window(), files);
    }

    @RequiredUIAccess
    private void open(Resolved resolved) {
        FileEditorWindow window = resolved.window();
        FileEditorManager fileEditorManager = FileEditorManager.getInstance(myProject);

        for (VirtualFile file : resolved.files()) {
            if (window != null && !window.isDisposed()) {
                FileEditor[] editors = fileEditorManager.openFileWithProviders(file, true, window).getFirst();
                if (editors.length > 0) {
                    continue;
                }
            }

            new OpenFileDescriptorImpl(myProject, file).navigate(true);
        }
    }

    @RequiredUIAccess
    private @Nullable FileEditorWindow findEditorWindow(@Nullable Editor editor) {
        if (editor == null || editor.isDisposed()) {
            return null;
        }

        VirtualFile file = FileDocumentManager.getInstance().getFile(editor.getDocument());
        if (file == null) {
            return null;
        }

        for (FileEditorWindow window : FileEditorManager.getInstance(myProject).getWindows()) {
            FileEditorWithProviderComposite composite = window.findFileComposite(file);
            if (composite == null) {
                continue;
            }

            for (FileEditor fileEditor : composite.getEditors()) {
                if (fileEditor instanceof TextEditor textEditor && textEditor.getEditor() == editor) {
                    return window;
                }
            }
        }
        return null;
    }
}
