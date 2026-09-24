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

import consulo.execution.service.ServiceEventListener;
import consulo.execution.service.ServiceViewContributor;
import consulo.execution.service.ServiceViewDescriptor;
import consulo.execution.service.ServiceViewManager;
import consulo.project.Project;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.JBTreeTraverser;
import consulo.util.concurrent.AsyncPromise;
import consulo.util.concurrent.Promise;
import consulo.util.concurrent.Promises;
import consulo.util.lang.Comparing;
import org.jspecify.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.function.Function;

/**
 * The services of a model as a tree, and the details of the selected one - the counterpart of the swing
 * {@link ServiceTreeView}.
 *
 * @author VISTALL
 * @since 2026-09-24
 */
final class UnifiedServiceTreeView extends UnifiedServiceView {
    private final UIAccess myUIAccess;
    private final UnifiedServiceViewTree myTree;
    private final UnifiedServiceViewTreeModel myTreeModel;
    private final ServiceViewModel.ServiceViewModelListener myListener;

    private volatile @Nullable ServiceViewItem myLastSelection;
    private boolean mySelected;

    @RequiredUIAccess
    UnifiedServiceTreeView(Project project, ServiceViewModel model, UnifiedServiceViewUi ui, ServiceViewState state) {
        super(project, model, ui);
        myUIAccess = project.getUIAccess();

        myTreeModel = new UnifiedServiceViewTreeModel(model);
        myTree = new UnifiedServiceViewTree(myUIAccess, myTreeModel, this::getViewOptions, this);

        UnifiedServiceViewActionProvider actionProvider = UnifiedServiceViewActionProvider.getInstance();
        ui.setServiceToolbar(actionProvider);
        ui.setMasterComponent(myTree.getComponent(), actionProvider);

        myComponent.center(myUi.getComponent());

        myTree.addSelectionListener(this::onSelectionChanged);

        myListener = new ServiceViewTreeModelListener();
        model.addModelListener(myListener);
    }

    @Override
    public void dispose() {
        getModel().removeModelListener(myListener);
        super.dispose();
    }

    @Override
    public void saveState(ServiceViewState state) {
        super.saveState(state);
        myUi.saveState(state);
    }

    /**
     * The unified tree selects one row at most.
     */
    @Override
    public List<ServiceViewItem> getSelectedItems() {
        ServiceViewItem item = myTree.getSelectedItem();
        return item == null ? Collections.emptyList() : Collections.singletonList(item);
    }

    @Override
    public Promise<Void> select(Object service, Class<?> contributorClass) {
        return doSelect(service, contributorClass, false);
    }

    private Promise<Void> selectSafe(Object service, Class<?> contributorClass) {
        return doSelect(service, contributorClass, true);
    }

    private Promise<Void> doSelect(Object service, Class<?> contributorClass, boolean safe) {
        ServiceViewItem selectedItem = myLastSelection;
        if (selectedItem == null || !selectedItem.getValue().equals(service)) {
            AsyncPromise<Void> result = new AsyncPromise<>();
            Promise<List<ServiceViewItem>> pathPromise =
                safe ? myTreeModel.findPathSafe(service, contributorClass) : myTreeModel.findPath(service, contributorClass);
            pathPromise
                .onError(result::setError)
                .onSuccess(path -> myTree.promiseSelect(path).whenComplete((node, error) -> {
                    if (error != null) {
                        result.setError(error);
                    }
                    else {
                        result.setResult(null);
                    }
                }));
            return result;
        }
        return Promises.resolvedPromise();
    }

    @Override
    public Promise<Void> expand(Object service, Class<?> contributorClass) {
        AsyncPromise<Void> result = new AsyncPromise<>();
        myTreeModel.findPath(service, contributorClass)
            .onError(result::setError)
            .onSuccess(path -> myTree.promiseExpand(path).whenComplete((node, error) -> {
                if (error != null) {
                    result.setError(error);
                }
                else {
                    result.setResult(null);
                }
            }));
        return result;
    }

    @Override
    public Promise<Void> extract(Object service, Class<?> contributorClass) {
        AsyncPromise<Void> result = new AsyncPromise<>();
        myTreeModel.findPath(service, contributorClass)
            .onError(result::setError)
            .onSuccess(path -> {
                ServiceViewItem item = path.get(path.size() - 1);
                if (item instanceof ServiceModel.ServiceNode node && node.isLoaded() && !node.isChildrenInitialized()) {
                    // Initialize children on BGT before extract in order correctly determine whether it leaf or not and
                    // use appropriate ServiceViewUi.
                    node.getChildren();
                }
                myUIAccess.give(() -> {
                    ServiceViewManagerImpl manager = (ServiceViewManagerImpl) ServiceViewManager.getInstance(getProject());
                    List<ServiceViewItem> items = Collections.singletonList(item);
                    manager.extract(this, items, ServiceViewDragHelper.getTheOnlyRootContributor(items));
                    result.setResult(null);
                });
            });
        return result;
    }

    @RequiredUIAccess
    @Override
    public void onViewSelected() {
        mySelected = true;
        ServiceViewItem lastSelection = myLastSelection;
        if (lastSelection != null) {
            ServiceViewDescriptor descriptor = lastSelection.getViewDescriptor();
            onViewSelected(descriptor);
            myUi.setDetailsComponent(descriptor.getContentUIComponent());
        }
        else {
            myUi.setDetailsComponent(null);
        }
    }

    @Override
    public void onViewUnselected() {
        mySelected = false;
        ServiceViewItem lastSelection = myLastSelection;
        if (lastSelection != null) {
            lastSelection.getViewDescriptor().onNodeUnselected();
        }
    }

    /**
     * A unified tree cannot be focused from the code yet.
     */
    @Override
    public void jumpToServices() {
    }

    @RequiredUIAccess
    private void onSelectionChanged() {
        List<ServiceViewItem> selected = getSelectedItems();

        ServiceViewItem newSelection;
        ServiceViewDescriptor newDescriptor;
        if (selected.size() == 1) {
            newSelection = selected.get(0);
            newDescriptor = newSelection.getViewDescriptor();
        }
        else {
            newSelection = null;
            ServiceViewContributor<?> contributor = ServiceViewDragHelper.getTheOnlyRootContributor(selected);
            newDescriptor = contributor == null ? null : contributor.getViewDescriptor(getProject());
        }

        if (newSelection != null && newSelection.equals(myLastSelection)) {
            return;
        }

        ServiceViewItem lastSelection = myLastSelection;
        ServiceViewDescriptor oldDescriptor = lastSelection == null ? null : lastSelection.getViewDescriptor();
        if (Comparing.equal(newDescriptor, oldDescriptor)) {
            return;
        }

        if (oldDescriptor != null && mySelected) {
            oldDescriptor.onNodeUnselected();
        }

        myLastSelection = newSelection;

        if (!mySelected) {
            return;
        }

        if (newDescriptor != null) {
            newDescriptor.onNodeSelected(ContainerUtil.map(selected, ServiceViewItem::getValue));
        }
        myUi.setDetailsComponent(newDescriptor == null ? null : newDescriptor.getContentUIComponent());
    }

    /**
     * A refresh of the unified tree drops its selection for a while, so the last selection - not the rows - tells
     * whether nothing was selected.
     */
    private void selectFirstItemIfNeeded() {
        myUIAccess.give(() -> {
            if (myLastSelection == null && getSelectedItems().isEmpty()) {
                ServiceViewItem item = ContainerUtil.getFirstItem(getModel().getRoots());
                if (item != null) {
                    select(item.getValue(), item.getRootContributor().getClass());
                }
            }
        });
    }

    /**
     * The selected service was changed - the item of it is another instance now, with details of its own, and the
     * actions of the toolbars may be enabled differently.
     */
    private void updateLastSelection() {
        ServiceViewItem lastSelection = myLastSelection;
        WeakReference<ServiceViewItem> itemRef =
            new WeakReference<>(lastSelection == null ? null : getModel().findItemSafe(lastSelection));
        myUIAccess.give(() -> {
            if (myLastSelection == null) {
                ServiceViewItem item = ContainerUtil.getFirstItem(getModel().getRoots());
                if (item != null) {
                    selectSafe(item.getValue(), item.getRootContributor().getClass());
                }
                return;
            }

            ServiceViewItem updatedItem = itemRef.get();
            if (updatedItem != null && Comparing.equal(updatedItem, myLastSelection)) {
                myLastSelection = updatedItem;
            }
            // Skip updating details component if updatedItem has been already marked as removed,
            // thus details component will be updated in the next already submitted update runnable.
            if (mySelected && (updatedItem == null || !updatedItem.isRemoved())) {
                ServiceViewItem selection = myLastSelection;
                ServiceViewDescriptor descriptor = selection == null || (selection.isRemoved() && updatedItem == null) ?
                    null : selection.getViewDescriptor();
                myUi.setDetailsComponent(descriptor == null ? null : descriptor.getContentUIComponent());
            }
        });
    }

    /**
     * The rows are built anew for the changed model, and the last selection is selected again by its value - the
     * item of an updated service is another instance.
     */
    private void updateSelectionPaths() {
        myUIAccess.give(() -> myTree.rootsChanged().thenRun(() -> {
            ServiceViewItem lastSelection = myLastSelection;
            if (lastSelection == null) {
                return;
            }

            myTreeModel.findPathSafe(lastSelection.getValue(), lastSelection.getRootContributor().getClass())
                .onSuccess(myTree::promiseSelect);
        }));
    }

    @Override
    public List<Object> getChildrenSafe(List<Object> valueSubPath, Class<?> contributorClass) {
        Queue<Object> values = new LinkedList<>(valueSubPath);
        Object visibleRoot = values.poll();
        if (visibleRoot == null) {
            return Collections.emptyList();
        }

        List<? extends ServiceViewItem> roots = getModel().getVisibleRoots();
        ServiceViewItem item = JBTreeTraverser.from((Function<ServiceViewItem, List<ServiceViewItem>>) node ->
                contributorClass.isInstance(node.getRootContributor()) ? new ArrayList<>(getModel().getChildren(node)) : null)
            .withRoots(roots)
            .traverse(ServiceModel.ONLY_LOADED_BFS)
            .filter(node -> node.getValue().equals(visibleRoot))
            .first();
        if (item == null) {
            return Collections.emptyList();
        }

        while (!values.isEmpty()) {
            Object value = values.poll();
            item = ContainerUtil.find(getModel().getChildren(item), child -> value.equals(child.getValue()));
            if (item == null) {
                return Collections.emptyList();
            }
        }
        return ContainerUtil.map(getModel().getChildren(item), ServiceViewItem::getValue);
    }

    private final class ServiceViewTreeModelListener implements ServiceViewModel.ServiceViewModelListener {
        @Override
        public void eventProcessed(ServiceEventListener.ServiceEvent e) {
            if (e.type == ServiceEventListener.EventType.UNLOAD_SYNC_RESET) {
                updateLastSelection();
            }
            else {
                ServiceViewItem lastSelection = myLastSelection;
                if (lastSelection != null && lastSelection.getRootContributor().getClass().equals(e.contributorClass)) {
                    updateLastSelection();
                }
                else {
                    selectFirstItemIfNeeded();
                }
            }
            updateSelectionPaths();
        }

        @Override
        public void structureChanged() {
            selectFirstItemIfNeeded();
            updateSelectionPaths();
        }
    }
}
