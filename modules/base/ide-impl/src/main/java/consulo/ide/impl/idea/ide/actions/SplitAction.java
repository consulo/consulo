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

import consulo.application.dumb.DumbAware;
import consulo.fileEditor.FileEditorWindow;
import consulo.fileEditor.internal.FileEditorManagerEx;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;

import javax.swing.*;

/**
 * @author Vladimir Kondratyev
 */
public abstract class SplitAction extends AnAction implements DumbAware {
    protected enum Orientation {
        HORIZONTAL(SwingConstants.HORIZONTAL),
        VERTICAL(SwingConstants.VERTICAL);

        public final int myOrientation;

        Orientation(int orientation) {
            myOrientation = orientation;
        }
    }

    @Nonnull
    private final Orientation myOrientation;

    protected SplitAction(@Nonnull LocalizeValue text, @Nonnull Image icon, @Nonnull Orientation orientation) {
        super(text, LocalizeValue.absent(), icon);
        myOrientation = orientation;
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent event) {
        Project project = event.getRequiredData(Project.KEY);
        FileEditorManagerEx fileEditorManager = FileEditorManagerEx.getInstanceEx(project);
        FileEditorWindow window = event.getData(FileEditorWindow.DATA_KEY);

        fileEditorManager.createSplitter(myOrientation.myOrientation, window);
    }

    @Override
    public void update(AnActionEvent event) {
        Project project = event.getData(Project.KEY);
        Presentation presentation = event.getPresentation();
        if (project == null) {
            presentation.setEnabled(false);
            return;
        }
        FileEditorManagerEx fileEditorManager = FileEditorManagerEx.getInstanceEx(project);
        presentation.setEnabled(fileEditorManager.hasOpenedFile());
    }
}
