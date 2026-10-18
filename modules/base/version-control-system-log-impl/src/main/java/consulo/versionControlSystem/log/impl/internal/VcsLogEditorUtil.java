/*
 * Copyright 2000-2024 JetBrains s.r.o. and contributors.
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
package consulo.versionControlSystem.log.impl.internal;

import consulo.fileEditor.FileEditor;
import consulo.fileEditor.FileEditorManager;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.versionControlSystem.log.VcsLogUi;
import consulo.versionControlSystem.log.impl.internal.ui.AbstractVcsLogUi;
import consulo.versionControlSystem.log.impl.internal.ui.editor.VcsLogEditor;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Lookups of the log UIs shown in editor tabs.
 */
public final class VcsLogEditorUtil {
    private VcsLogEditorUtil() {
    }

    static Set<String> getLogIds(FileEditor editor) {
        Set<String> ids = new LinkedHashSet<>();
        for (AbstractVcsLogUi ui : getLogUis(editor)) {
            ids.add(ui.getId());
        }
        return ids;
    }

    static Set<String> findSelectedLogIds(Project project) {
        Set<String> ids = new LinkedHashSet<>();
        for (FileEditor editor : FileEditorManager.getInstance(project).getSelectedEditors()) {
            ids.addAll(getLogIds(editor));
        }
        return ids;
    }

    public static <T extends VcsLogUi> @Nullable T findVcsLogUi(FileEditor[] editors, Class<T> clazz) {
        for (FileEditor editor : editors) {
            for (AbstractVcsLogUi ui : getLogUis(editor)) {
                if (clazz.isInstance(ui)) {
                    return clazz.cast(ui);
                }
            }
        }
        return null;
    }

    @RequiredUIAccess
    static boolean selectLogUi(Project project, VcsLogUi ui) {
        FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
        VirtualFile file = findLogFile(fileEditorManager, ui);
        return file != null && fileEditorManager.openFile(file, true, true).length > 0;
    }

    static void updateTabName(Project project, VcsLogUi ui) {
        FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
        VirtualFile file = findLogFile(fileEditorManager, ui);
        if (file != null) {
            fileEditorManager.updateFilePresentation(file);
        }
    }

    private static @Nullable VirtualFile findLogFile(FileEditorManager fileEditorManager, VcsLogUi ui) {
        for (FileEditor editor : fileEditorManager.getAllEditors()) {
            if (getLogIds(editor).contains(ui.getId())) {
                return editor.getFile();
            }
        }
        return null;
    }

    @RequiredUIAccess
    static boolean closeLogTabs(Project project, List<String> editorTabIds) {
        if (editorTabIds.isEmpty()) {
            return true;
        }
        Set<String> tabsToClose = new HashSet<>(editorTabIds);

        FileEditorManager editorManager = FileEditorManager.getInstance(project);
        for (FileEditor logEditor : editorManager.getAllEditors()) {
            Set<String> ids = getLogIds(logEditor);
            ids.retainAll(tabsToClose);
            if (ids.isEmpty()) {
                continue;
            }

            List<String> disposedIds = disposeLogUis(logEditor);
            VirtualFile file = logEditor.getFile();
            if (file != null) {
                project.getApplication().invokeLater(() -> editorManager.closeFile(file), project.getDisposed());
            }
            disposedIds.forEach(tabsToClose::remove);
        }
        return tabsToClose.isEmpty();
    }

    @RequiredUIAccess
    static List<String> disposeLogUis(FileEditor editor) {
        List<String> disposedIds = new ArrayList<>();
        for (AbstractVcsLogUi ui : getLogUis(editor)) {
            disposedIds.add(ui.getId());
        }
        if (editor instanceof VcsLogEditor logEditor) {
            logEditor.disposeLogUis();
        }
        return disposedIds;
    }

    private static List<AbstractVcsLogUi> getLogUis(FileEditor editor) {
        return editor instanceof VcsLogEditor logEditor ? logEditor.getFile().getLogUis() : List.of();
    }
}
