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
package consulo.fileEditor.action;

import consulo.application.dumb.DumbAware;
import consulo.fileEditor.FileEditorComposite;
import consulo.fileEditor.FileEditorManager;
import consulo.fileEditor.FileEditorWindow;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.undoRedo.CommandProcessor;
import consulo.util.lang.Pair;

import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;

/**
 * @author yole
 */
public abstract class CloseEditorsActionBase extends AnAction implements DumbAware {
    @Nonnull
    protected List<Pair<FileEditorComposite, FileEditorWindow>> getFilesToClose(final AnActionEvent event) {
        final ArrayList<Pair<FileEditorComposite, FileEditorWindow>> res = new ArrayList<>();
        final Project project = event.getData(Project.KEY);
        final FileEditorManager editorManager = FileEditorManager.getInstance(project);
        final FileEditorWindow editorWindow = event.getData(FileEditorWindow.DATA_KEY);
        final FileEditorWindow[] windows;
        if (editorWindow != null) {
            windows = new FileEditorWindow[]{editorWindow};
        }
        else {
            windows = editorManager.getWindows();
        }
        for (int i = 0; i != windows.length; ++i) {
            final FileEditorWindow window = windows[i];
            final FileEditorComposite[] editors = window.getEditors();
            for (final FileEditorComposite editor : editors) {
                if (isFileToClose(editor, window)) {
                    res.add(Pair.create(editor, window));
                }
            }
        }
        return res;
    }

    protected abstract boolean isFileToClose(FileEditorComposite editor, FileEditorWindow window);

    @RequiredUIAccess
    @Override
    public void actionPerformed(final AnActionEvent e) {
        final Project project = e.getData(Project.KEY);
        final CommandProcessor commandProcessor = CommandProcessor.getInstance();
        final FileEditorWindow editorWindow = e.getData(FileEditorWindow.DATA_KEY);
        final boolean inSplitter = editorWindow != null && editorWindow.inSplitter();
        commandProcessor.executeCommand(
            project,
            () -> {
                List<Pair<FileEditorComposite, FileEditorWindow>> filesToClose = getFilesToClose(e);
                for (int i = 0; i != filesToClose.size(); ++i) {
                    final Pair<FileEditorComposite, FileEditorWindow> we = filesToClose.get(i);
                    we.getSecond().closeFile(we.getFirst().getFile());
                }
            },
            getPresentationText(inSplitter),
            null
        );
    }

    @RequiredUIAccess
    @Override
    public void update(final AnActionEvent event) {
        final Presentation presentation = event.getPresentation();
        final FileEditorWindow editorWindow = event.getData(FileEditorWindow.DATA_KEY);
        final boolean inSplitter = editorWindow != null && editorWindow.inSplitter();
        presentation.setText(getPresentationText(inSplitter));
        final Project project = event.getData(Project.KEY);
        boolean enabled = (project != null && isActionEnabled(project, event));
        if (ActionPlaces.isPopupPlace(event.getPlace())) {
            presentation.setVisible(enabled);
        }
        else {
            presentation.setEnabled(enabled);
        }
    }

    protected boolean isActionEnabled(final Project project, final AnActionEvent event) {
        return getFilesToClose(event).size() > 0;
    }

    protected abstract String getPresentationText(boolean inSplitter);
}
