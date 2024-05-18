// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.execution.impl.internal.service;

import consulo.ui.ex.awt.tree.BaseTreeModel;
import consulo.ui.ex.util.Invoker;
import consulo.ui.ex.util.InvokerSupplier;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.JBTreeTraverser;
import consulo.util.concurrent.AsyncPromise;
import consulo.util.concurrent.Promise;
import consulo.util.lang.ObjectUtil;
import jakarta.annotation.Nonnull;

import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

final class ServiceViewTreeModel extends BaseTreeModel<Object> implements InvokerSupplier {
  private final ServiceViewModel myModel;
  private final Object myRoot = ObjectUtil.sentinel("services root");

  ServiceViewTreeModel(@Nonnull ServiceViewModel model) {
    myModel = model;
  }

  @Nonnull
  @Override
  public Invoker getInvoker() {
    return myModel.getInvoker();
  }

  @Override
  public void dispose() {
  }

  @Override
  public boolean isLeaf(Object object) {
    if (object == myRoot) return false;

    if (object instanceof ServiceModel.ServiceNode node) {
      if (!node.isChildrenInitialized() && !node.isLoaded()) {
        return false;
      }
    }
    return myModel.getChildren(((ServiceViewItem)object)).isEmpty();
  }

  @Override
  public List<?> getChildren(Object parent) {
    if (parent == myRoot) {
      return myModel.getVisibleRoots();
    }
    return myModel.getChildren(((ServiceViewItem)parent));
  }

  @Override
  public Object getRoot() {
    return myRoot;
  }

  void rootsChanged() {
    treeStructureChanged(new TreePath(myRoot), null, null);
  }

  Promise<TreePath> findPath(@Nonnull Object service, @Nonnull Class<?> contributorClass) {
    return doFindPath(service, contributorClass, false);
  }

  Promise<TreePath> findPathSafe(@Nonnull Object service, @Nonnull Class<?> contributorClass) {
    return doFindPath(service, contributorClass, true);
  }

  private Promise<TreePath> doFindPath(@Nonnull Object service, @Nonnull Class<?> contributorClass, boolean safe) {
    AsyncPromise<TreePath> result = new AsyncPromise<>();
    getInvoker().invoke(() -> {
      List<? extends ServiceViewItem> roots = myModel.getVisibleRoots();
      ServiceViewItem serviceNode = JBTreeTraverser.from((Function<ServiceViewItem, List<ServiceViewItem>>)node ->
                                                     contributorClass.isInstance(node.getRootContributor()) ? new ArrayList<>(myModel.getChildren(node)) : null)
                                                   .withRoots(roots)
                                                   .traverse(safe ? ServiceModel.ONLY_LOADED_BFS : ServiceModel.NOT_LOADED_LAST_BFS)
                                                   .filter(node -> node.getValue().equals(service))
                                                   .first();
      if (serviceNode != null) {
        List<Object> path = new ArrayList<>();
        do {
          path.add(serviceNode);
          serviceNode = roots.contains(serviceNode) ? null : serviceNode.getParent();
        }
        while (serviceNode != null);
        path.add(myRoot);
        Collections.reverse(path);
        result.setResult(new TreePath(ArrayUtil.toObjectArray(path)));
        return;
      }

      result.setError("Service not found");
    });
    return result;
  }
}
