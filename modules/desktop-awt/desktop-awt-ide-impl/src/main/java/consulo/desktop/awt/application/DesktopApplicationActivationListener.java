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
package consulo.desktop.awt.application;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicImpl;
import consulo.application.SaveAndSyncHandler;
import consulo.project.ui.wm.IdeFrame;
import consulo.project.ui.wm.event.ApplicationActivationListener;
import jakarta.inject.Inject;

/**
 * @author VISTALL
 * @since 2025-02-22
 */
@TopicImpl(ComponentScope.APPLICATION)
public class DesktopApplicationActivationListener implements ApplicationActivationListener {
    private final DesktopSaveAndSyncHandlerImpl mySaveAndSyncHandler;

    @Inject
    public DesktopApplicationActivationListener(SaveAndSyncHandler saveAndSyncHandler) {
        mySaveAndSyncHandler = (DesktopSaveAndSyncHandlerImpl) saveAndSyncHandler;
    }

    @Override
    public void applicationActivated(IdeFrame ideFrame) {
        mySaveAndSyncHandler.onFrameActivated();
    }

    @Override
    public void applicationDeactivated(IdeFrame ideFrame) {
        mySaveAndSyncHandler.onFrameDeactivated();
    }
}
