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
package consulo.desktop.awt.wm.navigationToolbar;

import consulo.annotation.component.ServiceImpl;
import consulo.dataContext.DataContext;
import consulo.desktop.awt.navbar.ui.NavBarUIController;
import consulo.desktop.awt.navbar.ui.NewNavBarPanel;
import consulo.navigationBar.internal.EmbeddedNavService;
import consulo.project.Project;
import consulo.ui.ex.awt.UIExAWTDataKey;
import consulo.ui.ex.awt.UIUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.awt.*;

/**
 * @author VISTALL
 * @since 2024-11-27
 */
@Singleton
@ServiceImpl
public class DesktopAWTEmbeddedNavService implements EmbeddedNavService {
    private final Project myProject;

    @Inject
    public DesktopAWTEmbeddedNavService(Project project) {
        myProject = project;
    }

    @Override
    public void show(DataContext context) {
        Component component = context.getData(UIExAWTDataKey.CONTEXT_COMPONENT);
        
        if (!isInsideNavBar(component)) {
            NavBarUIController.getInstance(myProject).showFloatingNavbar(context);
        }
    }

    private static boolean isInsideNavBar(Component c) {
        return c == null
            || c instanceof NewNavBarPanel
            || UIUtil.getParentOfType(NewNavBarPanel.class, c) != null;
    }
}
