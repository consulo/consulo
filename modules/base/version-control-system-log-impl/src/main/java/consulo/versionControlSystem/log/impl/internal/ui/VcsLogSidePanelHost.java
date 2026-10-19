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
package consulo.versionControlSystem.log.impl.internal.ui;

import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.project.ProjectPropertiesComponent;
import consulo.ui.Button;
import consulo.ui.ButtonStyle;
import consulo.ui.Component;
import consulo.ui.Space;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionToolbar;
import consulo.ui.ex.action.ActionToolbarFactory;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.SplitLayoutPosition;
import consulo.ui.layout.TwoComponentSplitLayout;
import consulo.versionControlSystem.log.VcsLogSidePanelProvider;
import consulo.versionControlSystem.log.VcsLogUi;
import consulo.versionControlSystem.log.localize.VersionControlSystemLogLocalize;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-10-19
 */
final class VcsLogSidePanelHost {
    private class HideAction extends DumbAwareAction {
        private HideAction() {
            super(VersionControlSystemLogLocalize.actionSidePanelHideText(), LocalizeValue.empty(), PlatformIconGroup.generalArrowleft());
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(AnActionEvent e) {
            setShown(false);
        }
    }

    private static final String SHOWN_KEY = "vcs.log.side.panel.shown.";
    private static final String PROPORTION_KEY = "vcs.log.side.panel.proportion.";
    private static final int DEFAULT_PROPORTION = 25;

    private final Project myProject;
    private final VcsLogUi myLogUi;
    private final VcsLogSidePanelProvider myProvider;
    private final Disposable myParentDisposable;
    private final DockLayout myStripe;
    private final DockLayout myPanel;
    private final TwoComponentSplitLayout mySplit;
    private final DockLayout myContainer;
    private final Component myLogComponent;
    private final ActionToolbar myToolbar;

    private @Nullable Component myContent;

    @RequiredUIAccess
    VcsLogSidePanelHost(Project project,
                        VcsLogUi logUi,
                        VcsLogSidePanelProvider provider,
                        Component logComponent,
                        Disposable parentDisposable) {
        myProject = project;
        myLogUi = logUi;
        myProvider = provider;
        myParentDisposable = parentDisposable;

        Button stripeButton = Button.create(provider.getTitle(), event -> setShown(true));
        stripeButton.setIcon(PlatformIconGroup.generalArrowright());
        stripeButton.addStyle(ButtonStyle.VERTICAL);

        myStripe = DockLayout.create(Space.NONE);
        myStripe.top(stripeButton);
        myStripe.borderBuilder().rightSet().apply();

        ActionGroup.Builder toolbarGroup = ActionGroup.newImmutableBuilder();
        toolbarGroup.add(new HideAction());
        ActionGroup providerActions = provider.getToolbarActions();
        if (providerActions != null) {
            toolbarGroup.addSeparator();
            toolbarGroup.add(providerActions);
        }
        ActionToolbar toolbar = ActionToolbarFactory.getInstance()
            .createActionToolbar("VcsLogSidePanel", toolbarGroup.build(), ActionToolbar.Style.VERTICAL);
        myToolbar = toolbar;

        DockLayout toolbarColumn = DockLayout.create(Space.NONE);
        toolbarColumn.center(toolbar.getUIComponent());
        toolbarColumn.borderBuilder().rightSet().apply();

        myPanel = DockLayout.create(Space.NONE);
        myPanel.left(toolbarColumn);
        toolbar.setTargetUIComponent(myPanel);

        ProjectPropertiesComponent properties = ProjectPropertiesComponent.getInstance(project);
        myLogComponent = logComponent;
        myContainer = DockLayout.create(Space.NONE);
        mySplit = TwoComponentSplitLayout.create(SplitLayoutPosition.HORIZONTAL)
            .withProportion(properties.getInt(PROPORTION_KEY + provider.getId(), DEFAULT_PROPORTION));
        mySplit.addSplitProportionChangedListener(event ->
            properties.setValue(PROPORTION_KEY + provider.getId(), event.getProportion(), DEFAULT_PROPORTION));

        setShown(properties.getBoolean(SHOWN_KEY + provider.getId(), false));
    }

    Component getStripe() {
        return myStripe;
    }

    Component getComponent() {
        return myContainer;
    }

    @RequiredUIAccess
    private void setShown(boolean shown) {
        if (shown && myContent == null) {
            Component content = myProvider.createComponent(myLogUi, myParentDisposable);
            myContent = content;
            myPanel.center(content);
            myToolbar.setTargetUIComponent(content);
        }

        if (shown) {
            myContainer.center(mySplit);
            mySplit.setFirstComponent(myPanel);
            mySplit.setSecondComponent(myLogComponent);
        }
        else {
            mySplit.setFirstComponent(null);
            mySplit.setSecondComponent(null);
            myContainer.center(myLogComponent);
        }
        myStripe.setVisible(!shown);

        ProjectPropertiesComponent.getInstance(myProject).setValue(SHOWN_KEY + myProvider.getId(), shown, false);
    }
}
