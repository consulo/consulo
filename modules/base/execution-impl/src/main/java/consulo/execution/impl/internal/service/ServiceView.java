// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.execution.impl.internal.service;

import consulo.application.HelpManager;
import consulo.dataContext.DataManager;
import consulo.dataContext.UiDataProvider;
import consulo.disposer.Disposable;
import consulo.execution.service.ServiceViewActionUtils;
import consulo.execution.service.ServiceViewDescriptor;
import consulo.execution.service.ServiceViewOptions;
import consulo.language.editor.PlatformDataKeys;
import consulo.navigation.Navigatable;
import consulo.project.Project;
import consulo.ui.ex.CopyProvider;
import consulo.ui.ex.awt.AutoScrollToSourceHandler;
import consulo.util.collection.ContainerUtil;
import consulo.util.concurrent.Promise;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.stream.Collectors;

public abstract class ServiceView extends JPanel implements Disposable {
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

    Project getProject() {
        return myProject;
    }

    public ServiceViewModel getModel() {
        return myModel;
    }

    ServiceViewUi getUi() {
        return myUi;
    }

    void saveState(ServiceViewState state) {
        myModel.saveState(state);
    }

    abstract List<ServiceViewItem> getSelectedItems();

    abstract Promise<Void> select(Object service, Class<?> contributorClass);

    abstract Promise<Void> expand(Object service, Class<?> contributorClass);

    abstract Promise<Void> extract(Object service, Class<?> contributorClass);

    abstract void onViewSelected();

    abstract void onViewUnselected();

    public boolean isGroupByServiceGroups() {
        return myModel.isGroupByServiceGroups();
    }

    public void setGroupByServiceGroups(boolean value) {
        myModel.setGroupByServiceGroups(value);
    }

    public boolean isGroupByContributor() {
        return myModel.isGroupByContributor();
    }

    public void setGroupByContributor(boolean value) {
        myModel.setGroupByContributor(value);
    }

    abstract List<Object> getChildrenSafe(List<Object> valueSubPath, Class<?> contributorClass);

    void setAutoScrollToSourceHandler(AutoScrollToSourceHandler autoScrollToSourceHandler) {
        myAutoScrollToSourceHandler = autoScrollToSourceHandler;
    }

    void onViewSelected(ServiceViewDescriptor descriptor) {
        descriptor.onNodeSelected(ContainerUtil.map(getSelectedItems(), ServiceViewItem::getValue));
        if (myAutoScrollToSourceHandler != null) {
            myAutoScrollToSourceHandler.onMouseClicked(this);
        }
    }

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
        ServiceViewOptions viewOptions = new ServiceViewOptions() {
            @Override
            public boolean isGroupByContributor() {
                return serviceView.isGroupByContributor();
            }

            @Override
            public boolean isGroupByServiceGroups() {
                return serviceView.isGroupByServiceGroups();
            }
        };

        DataManager.registerUiDataProvider(serviceView, sink -> {
            sink.set(HelpManager.HELP_ID, ServiceViewManagerImpl.getToolWindowContextHelpId());

            List<ServiceViewItem> selectedItems = serviceView.getSelectedItems();
            sink.set(PlatformDataKeys.SELECTED_ITEMS, ContainerUtil.map2Array(selectedItems, ServiceViewItem::getValue));

            ServiceViewItem item = ContainerUtil.getOnlyItem(selectedItems);
            if (item != null) {
                sink.set(PlatformDataKeys.SELECTED_ITEM, item);
            }

            sink.set(ServiceViewActionProvider.SERVICES_SELECTED_ITEMS, selectedItems);
            sink.set(CopyProvider.KEY, new ServiceViewCopyProvider(serviceView));
            sink.set(ServiceViewActionUtils.CONTRIBUTORS_KEY, serviceView.getModel()
                .getRoots()
                .stream()
                .map(ServiceViewItem::getRootContributor)
                .collect(Collectors.toSet())
            );
            sink.set(ServiceViewActionUtils.OPTIONS_KEY, viewOptions);

            sink.lazy(Navigatable.KEY_OF_ARRAY, () -> {
                List<Navigatable> navigatables = ContainerUtil.mapNotNull(selectedItems, it -> it.getViewDescriptor().getNavigatable());
                return navigatables.toArray(Navigatable.EMPTY_ARRAY);
            });


            ServiceViewItem selectedItem = ContainerUtil.getOnlyItem(selectedItems);
            ServiceViewDescriptor descriptor = selectedItem == null || selectedItem.isRemoved() ? null : selectedItem.getViewDescriptor();
            if (descriptor instanceof UiDataProvider uiDataProvider) {
                uiDataProvider.uiDataSnapshot(sink);
            }
        });
    }

    private static void setViewModelState(ServiceViewModel viewModel, ServiceViewState viewState) {
        viewModel.setGroupByServiceGroups(viewState.groupByServiceGroups);
        viewModel.setGroupByContributor(viewState.groupByContributor);
    }
}