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
import consulo.localize.LocalizeValue;
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
    protected List<Pair<FileEditorComposite, FileEditorWindow>> getFilesToClose(AnActionEvent event) {
        ArrayList<Pair<FileEditorComposite, FileEditorWindow>> res = new ArrayList<>();
        Project project = event.getRequiredData(Project.KEY);
        FileEditorManager editorManager = FileEditorManager.getInstance(project);
        FileEditorWindow editorWindow = event.getData(FileEditorWindow.DATA_KEY);
        FileEditorWindow[] windows;
        if (editorWindow != null) {
            windows = new FileEditorWindow[]{editorWindow};
        }
        else {
            windows = editorManager.getWindows();
        }
        for (int i = 0; i != windows.length; ++i) {
            FileEditorWindow window = windows[i];
            FileEditorComposite[] editors = window.getEditors();
            for (FileEditorComposite editor : editors) {
                if (isFileToClose(editor, window)) {
                    res.add(Pair.create(editor, window));
                }
            }
        }
        return res;
    }

    protected abstract boolean isFileToClose(FileEditorComposite editor, FileEditorWindow window);

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        CommandProcessor commandProcessor = CommandProcessor.getInstance();
        FileEditorWindow editorWindow = e.getData(FileEditorWindow.DATA_KEY);
        boolean inSplitter = editorWindow != null && editorWindow.inSplitter();
        commandProcessor.newCommand()
            .project(e.getData(Project.KEY))
            .name(getPresentationText(inSplitter))
            .run(() -> {
                List<Pair<FileEditorComposite, FileEditorWindow>> filesToClose = getFilesToClose(e);
                for (int i = 0; i != filesToClose.size(); ++i) {
                    Pair<FileEditorComposite, FileEditorWindow> we = filesToClose.get(i);
                    we.getSecond().closeFile(we.getFirst().getFile());
                }
            });
    }

    @Override
    public void update(@Nonnull AnActionEvent event) {
        Presentation presentation = event.getPresentation();
        FileEditorWindow editorWindow = event.getData(FileEditorWindow.DATA_KEY);
        boolean inSplitter = editorWindow != null && editorWindow.inSplitter();
        presentation.setTextValue(getPresentationText(inSplitter));
        Project project = event.getData(Project.KEY);
        boolean enabled = (project != null && isActionEnabled(project, event));
        if (ActionPlaces.isPopupPlace(event.getPlace())) {
            presentation.setVisible(enabled);
        }
        else {
            presentation.setEnabled(enabled);
        }
    }

    protected boolean isActionEnabled(Project project, AnActionEvent event) {
        return getFilesToClose(event).size() > 0;
    }

    @Nonnull
    protected abstract LocalizeValue getPresentationText(boolean inSplitter);
}
