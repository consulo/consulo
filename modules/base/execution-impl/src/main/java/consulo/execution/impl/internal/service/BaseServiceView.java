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

import consulo.application.HelpManager;
import consulo.dataContext.DataSink;
import consulo.dataContext.UiDataProvider;
import consulo.disposer.Disposable;
import consulo.execution.service.ServiceViewActionUtils;
import consulo.execution.service.ServiceViewDescriptor;
import consulo.execution.service.ServiceViewOptions;
import consulo.language.editor.PlatformDataKeys;
import consulo.navigation.Navigatable;
import consulo.project.Project;
import consulo.util.collection.ContainerUtil;
import consulo.util.concurrent.Promise;
import consulo.util.dataholder.Key;

import java.util.List;
import java.util.stream.Collectors;

/**
 * What {@link ServiceViewManagerImpl} and the actions of the services tool window know of a view - the swing
 * {@link ServiceView} and the {@link UnifiedServiceView} both are one.
 *
 * @author VISTALL
 * @since 2026-09-24
 */
public interface BaseServiceView extends Disposable {
    /**
     * The view of a content of the tool window, and the view the actions of the tool window are performed in.
     */
    Key<BaseServiceView> KEY = Key.create("services.view");

    Project getProject();

    ServiceViewModel getModel();

    void saveState(ServiceViewState state);

    List<ServiceViewItem> getSelectedItems();

    Promise<Void> select(Object service, Class<?> contributorClass);

    Promise<Void> expand(Object service, Class<?> contributorClass);

    Promise<Void> extract(Object service, Class<?> contributorClass);

    void onViewSelected();

    void onViewUnselected();

    boolean isGroupByServiceGroups();

    void setGroupByServiceGroups(boolean value);

    boolean isGroupByContributor();

    void setGroupByContributor(boolean value);

    List<Object> getChildrenSafe(List<Object> valueSubPath, Class<?> contributorClass);

    void jumpToServices();

    void setMasterComponentVisible(boolean visible);

    /**
     * What a view tells the actions - the selection, the contributors and the options of the view, and what the
     * descriptor of the selected item adds.
     */
    static void uiDataSnapshot(BaseServiceView serviceView, DataSink sink) {
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

        sink.set(KEY, serviceView);
        sink.set(HelpManager.HELP_ID, ServiceViewManagerImpl.getToolWindowContextHelpId());

        List<ServiceViewItem> selectedItems = serviceView.getSelectedItems();
        sink.set(PlatformDataKeys.SELECTED_ITEMS, ContainerUtil.map2Array(selectedItems, ServiceViewItem::getValue));

        ServiceViewItem item = ContainerUtil.getOnlyItem(selectedItems);
        if (item != null) {
            sink.set(PlatformDataKeys.SELECTED_ITEM, item);
        }

        sink.set(ServiceViewActionProvider.SERVICES_SELECTED_ITEMS, selectedItems);
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
    }
}
