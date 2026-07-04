/*
 * Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 *
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
package consulo.desktop.awt.navbar.ui;

import consulo.navigationBar.model.NavBarVm;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.util.ComponentUtil;
import org.jspecify.annotations.Nullable;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Window;

/**
 * Container of the navigation bar in the IDE frame: attaches itself to {@link NavBarService}
 * when shown ({@link #addNotify()}) and detaches when hidden ({@link #removeNotify()}).
 */
public final class StaticNavBarPanel extends JPanel {
    private final Project myProject;

    private @Nullable NewNavBarPanel myCurrentPanel;

    public StaticNavBarPanel(Project project) {
        super(new BorderLayout());
        myProject = project;
        setOpaque(false);
    }

    public Project getProject() {
        return myProject;
    }

    public @Nullable NavBarVm getModel() {
        NewNavBarPanel panel = myCurrentPanel;
        return panel != null ? panel.getVm() : null;
    }

    public @Nullable NewNavBarPanel getPanel() {
        return myCurrentPanel;
    }

    @RequiredUIAccess
    public void setPanel(@Nullable NewNavBarPanel panel) {
        NewNavBarPanel oldPanel = myCurrentPanel;
        if (oldPanel != null) {
            oldPanel.disconnect();
            remove(oldPanel);
        }
        myCurrentPanel = panel;
        if (panel != null) {
            add(panel, BorderLayout.CENTER);
        }
        revalidate();
        repaint();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        Window window = ComponentUtil.getWindow(this);
        if (window != null) {
            NavBarUIController.getInstance(myProject).attach(this, window);
        }
    }

    @Override
    public void removeNotify() {
        NavBarUIController.getInstance(myProject).detach(this);
        super.removeNotify();
    }
}
