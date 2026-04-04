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
package consulo.welcomeScreen.impl.internal.editor;

import consulo.configuration.editor.ConfigurationFileEditor;
import consulo.dataContext.DataManager;
import consulo.project.Project;
import consulo.project.internal.RecentProjectsManager;
import consulo.ui.Component;
import consulo.ui.Hyperlink;
import consulo.ui.Label;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.layout.SplitLayoutPosition;
import consulo.ui.layout.TwoComponentSplitLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 2026-03-08
 */
public class WelcomeConfigurationFileEditor extends ConfigurationFileEditor {
    private Component myComponent;

    public WelcomeConfigurationFileEditor(Project project, VirtualFile virtualFile) {
        super(project, virtualFile);
    }

    @Override
    @RequiredUIAccess
    public Component getUIComponent() {
        if (myComponent == null) {
            myComponent = createComponent();
        }
        return myComponent;
    }

    @Override
    @RequiredUIAccess
    public @Nullable Component getPreferredFocusedUIComponent() {
        return myComponent;
    }

    @RequiredUIAccess
    private Component createComponent() {
        TwoComponentSplitLayout layout = TwoComponentSplitLayout.create(SplitLayoutPosition.HORIZONTAL);
        layout.setProportion(50);

        // Left pane: recent projects
        VerticalLayout recentProjectsLayout = VerticalLayout.create();
        recentProjectsLayout.add(Label.create("Recent Projects"));

        RecentProjectsManager recentProjectsManager = RecentProjectsManager.getInstance();
        AnAction[] recentActions = recentProjectsManager.getRecentProjectsActions(RecentProjectsManager.RECENT_ACTIONS_USE_GROUPS_WELCOME_MENU);

        DataManager dataManager = DataManager.getInstance();

        for (AnAction action : recentActions) {
            if (action instanceof AnSeparator) {
                continue;
            }

            AnActionEvent event = AnActionEvent.createFromAnAction(
                action, null, ActionPlaces.WELCOME_SCREEN, dataManager.getDataContext(layout)
            );
            action.update(event);

            Presentation presentation = event.getPresentation();
            if (presentation.isVisible()) {
                Hyperlink link = Hyperlink.create(presentation.getTextValue(), e -> action.actionPerformed(event));
                recentProjectsLayout.add(link);
            }
        }

        layout.setFirstComponent(recentProjectsLayout);

        // Right pane: quick start actions
        VerticalLayout actionsLayout = VerticalLayout.create();
        actionsLayout.add(Label.create("Quick Start"));

        ActionManager actionManager = ActionManager.getInstance();
        ActionGroup quickStart = (ActionGroup) actionManager.getAction(IdeActions.GROUP_WELCOME_SCREEN_QUICKSTART);
        if (quickStart != null) {
            List<AnAction> actions = new ArrayList<>();
            collectAllActions(actions, quickStart);

            for (AnAction action : actions) {
                AnActionEvent event = AnActionEvent.createFromAnAction(
                    action, null, ActionPlaces.WELCOME_SCREEN, dataManager.getDataContext(layout)
                );
                action.update(event);

                Presentation presentation = event.getPresentation();
                if (presentation.isVisible()) {
                    Hyperlink link = Hyperlink.create(presentation.getTextValue(), e -> action.actionPerformed(event));
                    if (presentation.getIcon() != null) {
                        link.setIcon(presentation.getIcon());
                    }
                    actionsLayout.add(link);
                }
            }
        }

        layout.setSecondComponent(actionsLayout);

        return layout;
    }

    private static void collectAllActions(List<AnAction> result, ActionGroup group) {
        for (AnAction action : group.getChildren(null)) {
            if (action instanceof ActionGroup && !((ActionGroup) action).isPopup()) {
                collectAllActions(result, (ActionGroup) action);
            }
            else {
                result.add(action);
            }
        }
    }

    @Override
    public void dispose() {
    }
}
