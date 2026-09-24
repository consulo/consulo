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
package consulo.execution.impl.internal.service;

import consulo.dataContext.UiDataProvider;
import consulo.execution.localize.ExecutionLocalize;
import consulo.execution.service.ServiceViewActionUtils;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionToolbar;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.SplitLayoutPosition;
import consulo.ui.layout.TwoComponentSplitLayout;
import org.jspecify.annotations.Nullable;

/**
 * The tree of the services on the left, the details of the selected one on the right, and the toolbar of the item
 * before them - the counterpart of the swing {@link ServiceViewTreeUi}.
 *
 * @author VISTALL
 * @since 2026-09-24
 */
final class UnifiedServiceViewTreeUi implements UnifiedServiceViewUi {
    private final DockLayout myMainPanel;
    private final DockLayout myContentPanel;
    private final TwoComponentSplitLayout mySplitter;
    private final DockLayout myMasterPanel;
    private final DockLayout myDetailsPanel;
    private final DockLayout myContentComponentPanel;
    private final Component myMessagePanel;
    private @Nullable Component myDetailsComponent;
    /**
     * The split layout tells where its divider went only when it moves.
     */
    private int myContentProportion;
    private @Nullable ActionToolbar myServiceActionToolbar;
    private @Nullable ActionToolbar myMasterActionToolbar;

    @RequiredUIAccess
    UnifiedServiceViewTreeUi(ServiceViewState state) {
        myMainPanel = DockLayout.create();

        myContentPanel = DockLayout.create();
        myMainPanel.center(myContentPanel);

        myContentProportion = Math.round(state.contentProportion * 100);
        mySplitter = TwoComponentSplitLayout.create(SplitLayoutPosition.HORIZONTAL);
        mySplitter.setProportion(myContentProportion);
        mySplitter.addSplitProportionChangedListener(event -> myContentProportion = event.getProportion());
        myContentPanel.center(mySplitter);

        myMasterPanel = DockLayout.create();
        myMasterPanel.putUserData(UiDataProvider.KEY, sink -> sink.set(ServiceViewActionUtils.IS_FROM_TREE_KEY, true));
        mySplitter.setFirstComponent(myMasterPanel);

        myDetailsPanel = DockLayout.create();
        myContentComponentPanel = DockLayout.create();
        myMessagePanel = createMessagePanel();
        myContentComponentPanel.center(myMessagePanel);
        myDetailsPanel.center(myContentComponentPanel);
        mySplitter.setSecondComponent(myDetailsPanel);

        if (!state.showServicesTree) {
            myMasterPanel.setVisible(false);
        }
    }

    /**
     * The text of an empty selection in the middle of the details, as the swing panel draws its empty text.
     */
    @RequiredUIAccess
    private static Component createMessagePanel() {
        DockLayout messagePanel = DockLayout.create();
        messagePanel.center(Label.create(ExecutionLocalize.serviceViewEmptySelectionText()));
        return messagePanel;
    }

    @Override
    public Component getComponent() {
        return myMainPanel;
    }

    @Override
    public void saveState(ServiceViewState state) {
        state.contentProportion = myContentProportion / 100f;
    }

    @RequiredUIAccess
    @Override
    public void setServiceToolbar(UnifiedServiceViewActionProvider actionProvider) {
        boolean inDetails = ServiceViewUIUtils.isNewServicesUIEnabled();
        myServiceActionToolbar = actionProvider.createServiceToolbar(myMainPanel, inDetails);
        if (inDetails) {
            myDetailsPanel.top(actionProvider.wrapServiceToolbar(myServiceActionToolbar, inDetails));
        }
        else {
            myContentPanel.left(actionProvider.wrapServiceToolbar(myServiceActionToolbar, inDetails));
        }
    }

    @RequiredUIAccess
    @Override
    public void setMasterComponent(Component component, UnifiedServiceViewActionProvider actionProvider) {
        myMasterPanel.center(component);

        myMasterActionToolbar = actionProvider.createMasterComponentToolbar(component);
        myMasterPanel.top(myMasterActionToolbar.getUIComponent());

        actionProvider.installPopupHandler(component);
    }

    @RequiredUIAccess
    @Override
    public void setMasterComponentVisible(boolean visible) {
        myMasterPanel.setVisible(visible);
    }

    @RequiredUIAccess
    @Override
    public void setDetailsComponent(@Nullable Component component) {
        if (component != myDetailsComponent) {
            myDetailsComponent = component;
            myContentComponentPanel.center(component == null ? myMessagePanel : component);
        }

        ActionToolbar serviceActionToolbar = myServiceActionToolbar;
        if (serviceActionToolbar != null) {
            serviceActionToolbar.updateActionsAsync();
        }
        ActionToolbar masterActionToolbar = myMasterActionToolbar;
        if (masterActionToolbar != null) {
            masterActionToolbar.updateActionsAsync();
        }
    }

    @Override
    public @Nullable Component getDetailsComponent() {
        return myDetailsComponent;
    }
}
