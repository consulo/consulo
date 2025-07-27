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
import consulo.fileEditor.internal.FileEditorManagerEx;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import jakarta.annotation.Nonnull;

/**
 * @author yole
 */
public abstract class SplitterActionBase extends AnAction implements DumbAware {
    protected SplitterActionBase(@Nonnull LocalizeValue text, @Nonnull LocalizeValue description) {
        super(text, description);
    }

    @Override
    public void update(@Nonnull AnActionEvent event) {
        Project project = event.getData(Project.KEY);
        Presentation presentation = event.getPresentation();
        boolean enabled = project != null && isActionEnabled(project);
        if (ActionPlaces.isPopupPlace(event.getPlace())) {
            presentation.setVisible(enabled);
        }
        else {
            presentation.setEnabled(enabled);
        }
    }

    protected boolean isActionEnabled(Project project) {
        FileEditorManagerEx fileEditorManager = FileEditorManagerEx.getInstanceEx(project);
        return fileEditorManager.isInSplitter();
    }
}
