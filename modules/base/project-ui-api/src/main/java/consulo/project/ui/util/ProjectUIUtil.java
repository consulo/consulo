/*
 * Copyright 2013-2022 consulo.io
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
package consulo.project.ui.util;

import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.project.ui.internal.ProjectIdeFocusManager;
import consulo.project.ui.wm.IdeFrame;
import consulo.project.ui.wm.WindowManager;
import consulo.ui.ex.AppIcon;
import jakarta.annotation.Nonnull;

import javax.swing.*;

/**
 * @author VISTALL
 * @since 21-Jul-22
 */
public class ProjectUIUtil {
    public static void focusProjectWindow(final Project p, boolean executeIfAppInactive) {
        JFrame f = WindowManager.getInstance().getFrame(p);
        if (f == null) {
            return;
        }
        
        if (executeIfAppInactive) {
            IdeFrame ideFrame = WindowManager.getInstance().getIdeFrame(p);
            if (ideFrame != null) {
                AppIcon.getInstance().requestFocus(ideFrame.getWindow());
            }
            f.toFront();
        }
        else {
            ProjectIdeFocusManager.getInstance(p).requestFocus(f, true);
        }
    }

    @Nonnull
    public static Project guessCurrentProject(JComponent component) {
        Project project = null;
        Project[] openProjects = ProjectManager.getInstance().getOpenProjects();
        if (openProjects.length > 0) {
            project = openProjects[0];
        }
        if (project == null) {
            DataContext dataContext = component == null ? DataManager.getInstance().getDataContext() : DataManager.getInstance().getDataContext(component);
            project = dataContext.getData(Project.KEY);
        }
        if (project == null) {
            project = ProjectManager.getInstance().getDefaultProject();
        }
        return project;
    }
}
