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
import consulo.codeEditor.Editor;
import consulo.dataContext.DataContext;
import consulo.ide.impl.idea.ide.navigationToolbar.EmbeddedNavService;
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
        final Component component = context.getData(UIExAWTDataKey.CONTEXT_COMPONENT);
        if (!isInsideNavBar(component)) {
            final Editor editor = context.getData(Editor.KEY);
            final NavBarPanel toolbarPanel = new NavBarPanel(myProject, false);
            toolbarPanel.showHint(editor, context);
        }
    }

    private static boolean isInsideNavBar(Component c) {
        return c == null
            || c instanceof NavBarPanel
            || UIUtil.getParentOfType(NavBarListWrapper.class, c) != null;
    }
}
