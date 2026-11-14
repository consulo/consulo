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
package consulo.ai.editor.impl.internal.plan;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicImpl;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.FileEditorManager;
import consulo.fileEditor.TextEditor;
import consulo.fileEditor.event.FileEditorManagerListener;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Inject;

/**
 * A plan an agent wrote opens with its review - the comments to send back, Build to implement it.
 *
 * @author VISTALL
 * @since 2026-11-14
 */
@TopicImpl(ComponentScope.PROJECT)
public class AIPlanReviewEditorListener implements FileEditorManagerListener {
    private final Project myProject;

    @Inject
    public AIPlanReviewEditorListener(Project project) {
        myProject = project;
    }

    @RequiredUIAccess
    @Override
    public void fileOpened(FileEditorManager source, VirtualFile file) {
        AIPlanReviewManager manager = AIPlanReviewManager.getInstance(myProject);
        if (!manager.isPlan(file)) {
            return;
        }
        for (FileEditor fileEditor : source.getEditors(file)) {
            if (fileEditor instanceof TextEditor textEditor) {
                manager.install(textEditor.getEditor(), file, textEditor);
            }
        }
    }
}
