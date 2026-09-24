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
import consulo.execution.service.ServiceViewDescriptor;
import consulo.execution.service.ServiceViewOptions;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;
import consulo.util.collection.ContainerUtil;
import consulo.util.concurrent.Promise;

import java.util.List;

/**
 * A view of a services tool window built of unified components - the counterpart of the swing {@link ServiceView}.
 *
 * @author VISTALL
 * @since 2026-09-24
 */
public abstract class UnifiedServiceView implements BaseServiceView {
    private final Project myProject;
    private final ServiceViewModel myModel;
    protected final UnifiedServiceViewUi myUi;
    protected final DockLayout myComponent;

    @RequiredUIAccess
    protected UnifiedServiceView(Project project, ServiceViewModel model, UnifiedServiceViewUi ui) {
        myProject = project;
        myModel = model;
        myUi = ui;
        myComponent = DockLayout.create();
    }

    public Component getUIComponent() {
        return myComponent;
    }

    @Override
    public void dispose() {
    }

    @Override
    public Project getProject() {
        return myProject;
    }

    @Override
    public ServiceViewModel getModel() {
        return myModel;
    }

    UnifiedServiceViewUi getUi() {
        return myUi;
    }

    @Override
    public void saveState(ServiceViewState state) {
        myModel.saveState(state);
    }

    @RequiredUIAccess
    @Override
    public void setMasterComponentVisible(boolean visible) {
        myUi.setMasterComponentVisible(visible);
    }

    @Override
    public boolean isGroupByServiceGroups() {
        return myModel.isGroupByServiceGroups();
    }

    @Override
    public void setGroupByServiceGroups(boolean value) {
        myModel.setGroupByServiceGroups(value);
    }

    @Override
    public boolean isGroupByContributor() {
        return myModel.isGroupByContributor();
    }

    @Override
    public void setGroupByContributor(boolean value) {
        myModel.setGroupByContributor(value);
    }

    ServiceViewOptions getViewOptions() {
        return new ServiceViewOptions() {
            @Override
            public boolean isGroupByContributor() {
                return UnifiedServiceView.this.isGroupByContributor();
            }

            @Override
            public boolean isGroupByServiceGroups() {
                return UnifiedServiceView.this.isGroupByServiceGroups();
            }
        };
    }

    void onViewSelected(ServiceViewDescriptor descriptor) {
        descriptor.onNodeSelected(ContainerUtil.map(getSelectedItems(), ServiceViewItem::getValue));
    }

    @RequiredUIAccess
    static UnifiedServiceView createView(Project project, ServiceViewModel viewModel, ServiceViewState viewState) {
        setViewModelState(viewModel, viewState);
        UnifiedServiceView serviceView = createTreeView(project, viewModel, viewState);
        setDataProvider(serviceView);
        return serviceView;
    }

    @RequiredUIAccess
    private static UnifiedServiceView createTreeView(Project project, ServiceViewModel model, ServiceViewState state) {
        return new UnifiedServiceTreeView(project, model, new UnifiedServiceViewTreeUi(state), state);
    }

    private static void setDataProvider(UnifiedServiceView serviceView) {
        serviceView.getUIComponent().putUserData(UiDataProvider.KEY, sink -> BaseServiceView.uiDataSnapshot(serviceView, sink));
    }

    private static void setViewModelState(ServiceViewModel viewModel, ServiceViewState viewState) {
        viewModel.setGroupByServiceGroups(viewState.groupByServiceGroups);
        viewModel.setGroupByContributor(viewState.groupByContributor);
    }

    @Override
    public abstract List<ServiceViewItem> getSelectedItems();

    @Override
    public abstract Promise<Void> select(Object service, Class<?> contributorClass);

    @Override
    public abstract Promise<Void> expand(Object service, Class<?> contributorClass);

    @Override
    public abstract Promise<Void> extract(Object service, Class<?> contributorClass);
}
