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
package consulo.codeEditor.impl.internal.floating;

import consulo.application.Application;
import consulo.codeEditor.localize.CodeEditorLocalize;
import consulo.configurable.internal.ShowConfigurableService;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;

/**
 * @author VISTALL
 * @since 2026-11-09
 */
public class CustomizeCodeFloatingToolbarAction extends DumbAwareAction {
    private static final String CUSTOMIZATION_CONFIGURABLE_ID = "preferences.customizations";

    public CustomizeCodeFloatingToolbarAction() {
        super(CodeEditorLocalize.actionFloatingCodeToolbarCustomizeText());
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        Application.get().getInstance(ShowConfigurableService.class).showAndSelect(e.getData(Project.KEY), CUSTOMIZATION_CONFIGURABLE_ID);
    }
}
