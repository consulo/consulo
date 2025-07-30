// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.openapi.wm.impl.status.widget;

import consulo.annotation.component.ActionImpl;
import consulo.localize.LocalizeValue;
import consulo.platform.base.localize.ActionLocalize;
import consulo.ui.ex.action.DumbAwareToggleAction;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.project.ui.impl.internal.wm.statusBar.StatusBarWidgetSettings;
import consulo.project.ui.impl.internal.wm.statusBar.StatusBarWidgetsManagerImpl;
import consulo.project.ui.internal.StatusBarEx;
import consulo.project.ui.wm.StatusBar;
import consulo.project.ui.wm.StatusBarWidgetFactory;
import consulo.project.ui.wm.StatusBarWidgetsManager;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.localize.UILocalize;
import consulo.util.collection.ContainerUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;

@ActionImpl(id = StatusBarWidgetsActionGroup.GROUP_ID)
public class StatusBarWidgetsActionGroup extends ActionGroup {
    public static final String GROUP_ID = "ViewStatusBarWidgetsGroup";

    public StatusBarWidgetsActionGroup() {
        super(ActionLocalize.groupViewstatusbarwidgetsgroupText(), ActionLocalize.groupViewstatusbarwidgetsgroupDescription());
        setPopup(true);
    }

    @Nonnull
    @Override
    public AnAction[] getChildren(@Nullable AnActionEvent e) {
        Project project = e != null ? e.getData(Project.KEY) : null;
        if (project == null) {
            return AnAction.EMPTY_ARRAY;
        }

        StatusBarWidgetsManagerImpl manager = (StatusBarWidgetsManagerImpl) StatusBarWidgetsManager.getInstance(project);
        Collection<AnAction> toggleActions =
            new ArrayList<>(ContainerUtil.map(manager.getWidgetFactories(), factory -> new ToggleWidgetAction(factory, manager)));
        toggleActions.add(AnSeparator.getInstance());
        toggleActions.add(new HideCurrentWidgetAction());
        return toggleActions.toArray(AnAction.EMPTY_ARRAY);
    }

    private static final class ToggleWidgetAction extends DumbAwareToggleAction {
        private final StatusBarWidgetFactory myWidgetFactory;
        private final StatusBarWidgetsManagerImpl myManager;

        private ToggleWidgetAction(@Nonnull StatusBarWidgetFactory widgetFactory, StatusBarWidgetsManagerImpl manager) {
            super(LocalizeValue.localizeTODO(widgetFactory.getDisplayName()));
            myWidgetFactory = widgetFactory;
            myManager = manager;
        }

        @Override
        public void update(@Nonnull AnActionEvent e) {
            super.update(e);
            Project project = e.getData(Project.KEY);
            if (project == null) {
                e.getPresentation().setEnabledAndVisible(false);
                return;
            }
            if (ActionPlaces.isMainMenuOrShortcut(e.getPlace())) {
                e.getPresentation().setEnabledAndVisible(myWidgetFactory.isConfigurable() && myWidgetFactory.isAvailable(project));
                return;
            }
            StatusBar statusBar = e.getData(StatusBar.KEY);
            e.getPresentation()
                .setEnabledAndVisible(statusBar != null && myManager.canBeEnabledOnStatusBar(myWidgetFactory, statusBar));
        }

        @Override
        public boolean isSelected(@Nonnull AnActionEvent e) {
            return StatusBarWidgetSettings.getInstance().isEnabled(myWidgetFactory);
        }

        @Override
        @RequiredUIAccess
        public void setSelected(@Nonnull AnActionEvent e, boolean state) {
            StatusBarWidgetSettings.getInstance().setEnabled(myWidgetFactory, state);
            for (Project project : ProjectManager.getInstance().getOpenProjects()) {
                StatusBarWidgetsManager.getInstance(project).updateWidget(myWidgetFactory, UIAccess.current());
            }
        }
    }

    private static class HideCurrentWidgetAction extends DumbAwareAction {
        @Override
        @RequiredUIAccess
        public void actionPerformed(@Nonnull AnActionEvent e) {
            StatusBarWidgetFactory factory = getFactory(e);
            if (factory == null) {
                return;
            }

            StatusBarWidgetSettings.getInstance().setEnabled(factory, false);
            for (Project project : ProjectManager.getInstance().getOpenProjects()) {
                StatusBarWidgetsManager.getInstance(project).updateWidget(factory, UIAccess.current());
            }
        }

        @Override
        public void update(@Nonnull AnActionEvent e) {
            super.update(e);
            StatusBarWidgetFactory factory = getFactory(e);
            e.getPresentation().setEnabledAndVisible(factory != null && factory.isConfigurable());
            if (factory != null) {
                e.getPresentation().setTextValue(UILocalize.statusBarHideWidgetActionName(factory.getDisplayName()));
            }
        }

        @Nullable
        private static StatusBarWidgetFactory getFactory(@Nonnull AnActionEvent e) {
            Project project = e.getData(Project.KEY);
            String hoveredWidgetId = e.getData(StatusBarEx.HOVERED_WIDGET_ID);
            if (project != null && hoveredWidgetId != null && e.hasData(StatusBar.KEY)) {
                return ((StatusBarWidgetsManagerImpl) StatusBarWidgetsManager.getInstance(project)).findWidgetFactory(hoveredWidgetId);
            }
            return null;
        }
    }
}
