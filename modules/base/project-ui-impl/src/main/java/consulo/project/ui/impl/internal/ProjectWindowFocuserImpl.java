/*
 * Copyright 2013-2024 consulo.io
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
package consulo.project.ui.impl.internal;

import consulo.annotation.component.ServiceImpl;
import consulo.project.Project;
import consulo.project.internal.ProjectWindowFocuser;
import consulo.project.ui.internal.ProjectIdeFocusManager;
import consulo.project.ui.wm.IdeFrame;
import consulo.project.ui.wm.WindowManager;
import consulo.ui.ex.AppIcon;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import javax.swing.*;

/**
 * @author VISTALL
 * @since 2024-08-04
 */
@ServiceImpl
@Singleton
public class ProjectWindowFocuserImpl implements ProjectWindowFocuser {
    private final Project myProject;

    @Inject
    public ProjectWindowFocuserImpl(Project project) {
        myProject = project;
    }

    @Override
    public void focusProjectWindow(boolean executeIfAppInactive) {
        JFrame f = WindowManager.getInstance().getFrame(myProject);
        if (f == null) {
            return;
        }

        if (executeIfAppInactive) {
            IdeFrame ideFrame = WindowManager.getInstance().getIdeFrame(myProject);
            if (ideFrame != null) {
                AppIcon.getInstance().requestFocus(ideFrame.getWindow());
            }
            f.toFront();
        }
        else {
            ProjectIdeFocusManager.getInstance(myProject).requestFocus(f, true);
        }
    }
}
