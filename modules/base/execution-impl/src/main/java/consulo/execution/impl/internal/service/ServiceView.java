// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.execution.impl.internal.service;

import consulo.dataContext.DataManager;
import consulo.execution.service.ServiceViewDescriptor;
import consulo.project.Project;
import consulo.ui.ex.CopyProvider;
import consulo.ui.ex.awt.AutoScrollToSourceHandler;
import consulo.util.collection.ContainerUtil;
import consulo.util.concurrent.Promise;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public abstract class ServiceView extends JPanel implements BaseServiceView {
    private final Project myProject;
    private final ServiceViewModel myModel;
    protected final ServiceViewUi myUi;
    private AutoScrollToSourceHandler myAutoScrollToSourceHandler;

    protected ServiceView(LayoutManager layout, Project project, ServiceViewModel model, ServiceViewUi ui) {
        super(layout);
        myProject = project;
        myModel = model;
        myUi = ui;
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

    ServiceViewUi getUi() {
        return myUi;
    }

    @Override
    public void saveState(ServiceViewState state) {
        myModel.saveState(state);
    }

    @Override
    public void setMasterComponentVisible(boolean visible) {
        myUi.setMasterComponentVisible(visible);
    }

    @Override
    public abstract List<ServiceViewItem> getSelectedItems();

    @Override
    public abstract Promise<Void> select(Object service, Class<?> contributorClass);

    @Override
    public abstract Promise<Void> expand(Object service, Class<?> contributorClass);

    @Override
    public abstract Promise<Void> extract(Object service, Class<?> contributorClass);

    @Override
    public abstract void onViewSelected();

    @Override
    public abstract void onViewUnselected();

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

    @Override
    public abstract List<Object> getChildrenSafe(List<Object> valueSubPath, Class<?> contributorClass);

    void setAutoScrollToSourceHandler(AutoScrollToSourceHandler autoScrollToSourceHandler) {
        myAutoScrollToSourceHandler = autoScrollToSourceHandler;
    }

    void onViewSelected(ServiceViewDescriptor descriptor) {
        descriptor.onNodeSelected(ContainerUtil.map(getSelectedItems(), ServiceViewItem::getValue));
        if (myAutoScrollToSourceHandler != null) {
            myAutoScrollToSourceHandler.onMouseClicked(this);
        }
    }

    @Override
    public abstract void jumpToServices();

    static ServiceView createView(Project project, ServiceViewModel viewModel, ServiceViewState viewState) {
        setViewModelState(viewModel, viewState);
        ServiceView serviceView = createTreeView(project, viewModel, viewState);
        setDataProvider(serviceView);
        return serviceView;
    }

    private static ServiceView createTreeView(Project project, ServiceViewModel model, ServiceViewState state) {
        return new ServiceTreeView(project, model, new ServiceViewTreeUi(state), state);
    }

    private static void setDataProvider(ServiceView serviceView) {
        DataManager.registerUiDataProvider(serviceView, sink -> {
            BaseServiceView.uiDataSnapshot(serviceView, sink);
            sink.set(CopyProvider.KEY, new ServiceViewCopyProvider(serviceView));
        });
    }

    private static void setViewModelState(ServiceViewModel viewModel, ServiceViewState viewState) {
        viewModel.setGroupByServiceGroups(viewState.groupByServiceGroups);
        viewModel.setGroupByContributor(viewState.groupByContributor);
    }
}
