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

import consulo.ui.TextItemPresentation;
import consulo.ui.TreeModel;
import consulo.ui.TreeNode;
import consulo.ui.ex.util.Invoker;
import consulo.ui.ex.util.InvokerSupplier;
import consulo.util.collection.JBTreeTraverser;
import consulo.util.concurrent.AsyncPromise;
import consulo.util.concurrent.Promise;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * The items of a services view as a unified tree - the counterpart of the swing {@link ServiceViewTreeModel}. The
 * levels are built on the invoker of the services model, which is the only thread the model is read on.
 *
 * @author VISTALL
 * @since 2026-09-24
 */
final class UnifiedServiceViewTreeModel implements TreeModel<ServiceViewItem>, InvokerSupplier {
    private final ServiceViewModel myModel;
    private BiConsumer<ServiceViewItem, TextItemPresentation> myRenderer = (item, presentation) -> presentation.append(item.toString());

    UnifiedServiceViewTreeModel(ServiceViewModel model) {
        myModel = model;
    }

    @Override
    public Invoker getInvoker() {
        return myModel.getInvoker();
    }

    void setRenderer(BiConsumer<ServiceViewItem, TextItemPresentation> renderer) {
        myRenderer = renderer;
    }

    boolean isLeaf(ServiceViewItem item) {
        if (item instanceof ServiceModel.ServiceNode node) {
            if (!node.isChildrenInitialized() && !node.isLoaded()) {
                return false;
            }
        }
        return myModel.getChildren(item).isEmpty();
    }

    @Override
    public void buildChildren(Function<ServiceViewItem, TreeNode<ServiceViewItem>> nodeFactory, @Nullable ServiceViewItem parentValue) {
        List<? extends ServiceViewItem> children = parentValue == null ? myModel.getVisibleRoots() : myModel.getChildren(parentValue);
        for (ServiceViewItem child : children) {
            TreeNode<ServiceViewItem> node = nodeFactory.apply(child);
            node.setLeaf(isLeaf(child));
            node.setRenderer(myRenderer);
        }
    }

    Promise<List<ServiceViewItem>> findPath(Object service, Class<?> contributorClass) {
        return doFindPath(service, contributorClass, false);
    }

    Promise<List<ServiceViewItem>> findPathSafe(Object service, Class<?> contributorClass) {
        return doFindPath(service, contributorClass, true);
    }

    /**
     * The items from a visible root down to the one of the service - the tree has no root item of its own.
     */
    private Promise<List<ServiceViewItem>> doFindPath(Object service, Class<?> contributorClass, boolean safe) {
        AsyncPromise<List<ServiceViewItem>> result = new AsyncPromise<>();
        getInvoker().invoke(() -> {
            List<? extends ServiceViewItem> roots = myModel.getVisibleRoots();
            ServiceViewItem serviceNode = JBTreeTraverser.from((Function<ServiceViewItem, List<ServiceViewItem>>) node ->
                    contributorClass.isInstance(node.getRootContributor()) ? new ArrayList<>(myModel.getChildren(node)) : null)
                .withRoots(roots)
                .traverse(safe ? ServiceModel.ONLY_LOADED_BFS : ServiceModel.NOT_LOADED_LAST_BFS)
                .filter(node -> node.getValue().equals(service))
                .first();
            if (serviceNode != null) {
                List<ServiceViewItem> path = new ArrayList<>();
                do {
                    path.add(serviceNode);
                    serviceNode = roots.contains(serviceNode) ? null : serviceNode.getParent();
                }
                while (serviceNode != null);
                Collections.reverse(path);
                result.setResult(path);
                return;
            }

            result.setError("Service not found");
        });
        return result;
    }
}
