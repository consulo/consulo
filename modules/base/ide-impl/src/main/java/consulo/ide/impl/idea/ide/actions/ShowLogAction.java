/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

import consulo.annotation.component.ActionImpl;
import consulo.platform.Platform;
import consulo.platform.PlatformFeature;
import consulo.ui.UIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.application.dumb.DumbAware;
import consulo.container.boot.ContainerPathManager;
import consulo.platform.base.localize.ActionLocalize;
import consulo.ui.annotation.RequiredUIAccess;

import jakarta.annotation.Nonnull;

import java.io.File;

/**
 * @author pegov
 */
@ActionImpl(id = "ShowLog")
public class ShowLogAction extends AnAction implements DumbAware {
    public ShowLogAction() {
        super(ActionLocalize.actionShowlogText(), ActionLocalize.actionShowlogDescription());
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        File logFile = new File(ContainerPathManager.get().getLogPath(), "consulo.log");
        Platform platform = Platform.current();
        platform.openFileInFileManager(logFile, UIAccess.current());
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        Platform platform = Platform.current();

        Presentation presentation = e.getPresentation();
        presentation.setVisible(platform.supportsFeature(PlatformFeature.OPEN_FILE_IN_FILE_MANANGER));
        presentation.setTextValue(ActionLocalize.showLogInActionText(platform.fileManagerName()));
    }
}
