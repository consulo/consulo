/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package consulo.ide.impl.idea.ide.actions;

import consulo.fileEditor.action.CloseEditorsActionBase;
import consulo.ide.localize.IdeLocalize;
import consulo.ui.ex.action.AnActionEvent;
import consulo.project.Project;
import consulo.fileEditor.FileEditorComposite;
import consulo.fileEditor.FileEditorWindow;
import consulo.fileEditor.FileEditorWithProviderComposite;
import consulo.util.lang.Pair;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author yole
 */
public class CloseAllUnpinnedEditorsAction extends CloseEditorsActionBase {
    @Override
    protected boolean isFileToClose(FileEditorComposite editor, FileEditorWindow window) {
        return !window.isFilePinned(editor.getFile());
    }

    @Override
    protected String getPresentationText(boolean inSplitter) {
        return inSplitter
            ? IdeLocalize.actionCloseAllUnpinnedEditorsInTabGroup().get()
            : IdeLocalize.actionCloseAllUnpinnedEditors().get();
    }

    @Override
    protected boolean isActionEnabled(Project project, AnActionEvent event) {
        List<Pair<FileEditorComposite, FileEditorWindow>> filesToClose = getFilesToClose(event);
        if (filesToClose.isEmpty()) {
            return false;
        }
        Set<FileEditorWindow> checked = new HashSet<>();
        for (Pair<FileEditorComposite, FileEditorWindow> pair : filesToClose) {
            FileEditorWindow window = pair.second;
            if (!checked.contains(window)) {
                checked.add(window);
                if (hasPinned(window)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean hasPinned(FileEditorWindow window) {
        for (FileEditorWithProviderComposite e : window.getEditors()) {
            if (e.isPinned()) {
                return true;
            }
        }
        return false;
    }
}