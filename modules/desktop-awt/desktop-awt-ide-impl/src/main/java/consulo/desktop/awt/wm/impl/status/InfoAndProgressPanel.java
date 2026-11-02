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
package consulo.desktop.awt.wm.impl.status;

import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.TaskInfo;
import consulo.disposer.Disposer;
import consulo.project.Project;
import consulo.project.ui.impl.internal.wm.statusBar.UnifiedInfoAndProgressPanel;
import consulo.project.ui.wm.CustomStatusBarWidget;
import consulo.project.ui.wm.StatusBar;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.lang.Pair;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * The progress widget of the awt status bar - the unified panel every other frontend shows in its status bar, placed
 * among the widgets.
 *
 * @author VISTALL
 * @since 2026-11-02
 */
public class InfoAndProgressPanel implements CustomStatusBarWidget {
    private final Project myProject;
    private final UnifiedInfoAndProgressPanel myPanel;

    @RequiredUIAccess
    public InfoAndProgressPanel(Project project) {
        myProject = project;
        myPanel = new UnifiedInfoAndProgressPanel(project::getUIAccess);
        Disposer.register(this, myPanel);
    }

    /**
     * A task announces itself from whatever thread starts it, and the panel it goes into is built of components.
     */
    public void addProgress(ProgressIndicator original, TaskInfo info) {
        myProject.getUIAccess().giveIfNeed(() -> myPanel.addProgress(original, info));
    }

    public List<Pair<TaskInfo, ProgressIndicator>> getBackgroundProcesses() {
        return myPanel.getBackgroundProcesses();
    }

    @RequiredUIAccess
    public boolean isProcessWindowOpen() {
        return myPanel.isProcessWindowOpen();
    }

    @RequiredUIAccess
    public void setBanners(List<Component> banners, boolean reveal) {
        myPanel.setBanners(banners, reveal);
    }

    @RequiredUIAccess
    public void setProcessWindowOpen(boolean open) {
        myPanel.setProcessWindowOpen(open);
    }

    @Override
    @RequiredUIAccess
    public Component getUIComponent() {
        return myPanel.getUIComponent();
    }

    @Override
    public String getId() {
        return StatusBar.PROGRESS_WIDGET_ID;
    }

    @Override
    public @Nullable WidgetPresentation getPresentation() {
        return null;
    }

    @Override
    public void install(StatusBar statusBar) {
    }

    @Override
    public void dispose() {
    }
}
