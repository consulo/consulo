/*
 * Copyright 2013-2025 consulo.io
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
package consulo.codeEditor.impl.internal.stickyLine;

import consulo.annotation.component.ActionImpl;
import consulo.codeEditor.impl.internal.setting.EditorAppearanceConfigurable;
import consulo.codeEditor.localize.CodeEditorLocalize;
import consulo.configurable.internal.ShowConfigurableService;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

/**
 * @author VISTALL
 * @since 2025-05-24
 */
@ActionImpl(id = "StickyLinesConfigureAction")
public class StickyLinesConfigureAction extends StickyLinesAbstractAction {
    @Nonnull
    private final Provider<ShowConfigurableService> myShowConfigurableService;

    @Inject
    public StickyLinesConfigureAction(@Nonnull Provider<ShowConfigurableService> showConfigurableService) {
        super(CodeEditorLocalize.actionEditorstickyconfigureText(), CodeEditorLocalize.actionEditorstickyconfigureDescription());
        myShowConfigurableService = showConfigurableService;
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(@Nonnull AnActionEvent e) {
        Project project = e.getData(Project.KEY);

        myShowConfigurableService.get().showAndSelect(project, EditorAppearanceConfigurable.class);
    }
}
