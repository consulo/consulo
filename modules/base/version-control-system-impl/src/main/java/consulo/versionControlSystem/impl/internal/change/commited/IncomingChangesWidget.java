/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.versionControlSystem.impl.internal.change.commited;

import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.project.ui.wm.StatusBar;
import consulo.project.ui.wm.StatusBarWidget;
import consulo.project.ui.wm.StatusBarWidgetFactory;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import consulo.versionControlSystem.VcsToolWindow;
import consulo.versionControlSystem.impl.internal.change.ui.awt.ChangesViewContentManager;
import consulo.versionControlSystem.localize.VcsLocalize;
import consulo.versionControlSystem.versionBrowser.CommittedChangeList;
import jakarta.annotation.Nonnull;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.Consumer;

class IncomingChangesWidget implements StatusBarWidget, StatusBarWidget.IconPresentation {
    private StatusBar myStatusBar;

    private Image myCurrentIcon = ImageEffects.grayed(PlatformIconGroup.ideIncomingchangeson());
    @Nonnull
    private LocalizeValue myToolTipText = LocalizeValue.absent();
    private final StatusBarWidgetFactory myFactory;
    private final IncomingChangesIndicator myIncomingChangesIndicator;

    IncomingChangesWidget(StatusBarWidgetFactory factory, IncomingChangesIndicator incomingChangesIndicator) {
        myFactory = factory;
        myIncomingChangesIndicator = incomingChangesIndicator;
    }

    void refreshIndicator() {
        List<CommittedChangeList> list = myIncomingChangesIndicator.getCache().getCachedIncomingChanges();
        if (list == null || list.isEmpty()) {
            clear();
        }
        else {
            setChangesAvailable(VcsLocalize.incomingChangesIndicatorTooltip(list.size()));
        }
    }

    void clear() {
        update(
            ImageEffects.grayed(PlatformIconGroup.ideIncomingchangeson()),
            LocalizeValue.localizeTODO("No incoming changelists available")
        );
    }

    void setChangesAvailable(@Nonnull LocalizeValue toolTipText) {
        update(PlatformIconGroup.ideIncomingchangeson(), toolTipText);
    }

    private void update(@Nonnull Image icon, @Nonnull LocalizeValue toolTipText) {
        myCurrentIcon = icon;
        myToolTipText = toolTipText;
        if (myStatusBar != null) {
            myStatusBar.updateWidget(getId());
        }
    }

    @Override
    @Nonnull
    public Image getIcon() {
        return myCurrentIcon;
    }

    @Nonnull
    @Override
    public LocalizeValue getTooltipText() {
        return myToolTipText;
    }

    @Override
    @RequiredUIAccess
    public Consumer<MouseEvent> getClickConsumer() {
        return mouseEvent -> {
            if (myStatusBar != null) {
                DataContext dataContext = DataManager.getInstance().getDataContext((Component) myStatusBar);
                Project project = dataContext.getData(Project.KEY);
                if (project != null) {
                    ToolWindow changesView = ToolWindowManager.getInstance(project).getToolWindow(VcsToolWindow.ID);
                    changesView.show(() -> ChangesViewContentManager.getInstance(project).selectContent("Incoming"));
                }
            }
        };
    }

    @Override
    @Nonnull
    public String getId() {
        return myFactory.getId();
    }

    @Override
    public WidgetPresentation getPresentation() {
        return this;
    }

    @Override
    public void install(@Nonnull StatusBar statusBar) {
        myStatusBar = statusBar;
    }

    @Override
    public void dispose() {
        myStatusBar = null;
    }
}
