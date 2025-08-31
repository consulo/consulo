// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.project.ui.view.tree;

import consulo.application.dumb.IndexNotReadyException;
import consulo.component.ProcessCanceledException;
import consulo.logging.Logger;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.ui.ex.tree.AbstractTreeStructure;
import consulo.ui.ex.tree.NodeDescriptor;
import consulo.util.collection.ArrayUtil;
import consulo.util.dataholder.Key;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static consulo.application.util.registry.Registry.is;

public abstract class AbstractTreeStructureBase extends AbstractTreeStructure {
  private static final Logger LOG = Logger.getInstance(AbstractTreeStructureBase.class);
  protected final Project myProject;

  protected AbstractTreeStructureBase(Project project) {
    myProject = project;
  }

  @Nonnull
  @Override
  public Object[] getChildElements(@Nonnull Object element) {
    LOG.assertTrue(element instanceof AbstractTreeNode, element.getClass().getName());
    AbstractTreeNode<?> treeNode = (AbstractTreeNode)element;
    Collection<? extends AbstractTreeNode> elements = treeNode.getChildren();
    if (elements.stream().anyMatch(Objects::isNull)) LOG.error("node contains null child: " + treeNode + "; " + treeNode.getClass());
    List<TreeStructureProvider> providers = is("allow.tree.structure.provider.in.dumb.mode") ? getProviders() : getProvidersDumbAware();
    if (providers != null && !providers.isEmpty()) {
      ViewSettings settings = treeNode instanceof ProjectViewNode ? ((ProjectViewNode)treeNode).getSettings() : ViewSettings.DEFAULT;
      for (TreeStructureProvider provider : providers) {
        try {
          elements = provider.modify(treeNode, (Collection<AbstractTreeNode>)elements, settings);
          if (elements.stream().anyMatch(Objects::isNull)) LOG.error("provider creates null child: " + provider);
        }
        catch (IndexNotReadyException e) {
          LOG.debug("TreeStructureProvider.modify requires indices", e);
          throw new ProcessCanceledException(e);
        }
        catch (ProcessCanceledException e) {
          throw e;
        }
        catch (Throwable e) {
          LOG.error(e);
        }
      }
    }
    elements.forEach(node -> node.setParent(treeNode));
    return ArrayUtil.toObjectArray(elements);
  }

  @Override
  public boolean isValid(@Nonnull Object element) {
    return element instanceof AbstractTreeNode;
  }

  @Override
  public Object getParentElement(@Nonnull Object element) {
    if (element instanceof AbstractTreeNode) {
      return ((AbstractTreeNode)element).getParent();
    }
    return null;
  }

  @Override
  @Nonnull
  public NodeDescriptor createDescriptor(@Nonnull Object element, NodeDescriptor parentDescriptor) {
    return (NodeDescriptor)element;
  }

  @Nullable
  public abstract List<TreeStructureProvider> getProviders();

  @Nullable
  public Object getDataFromProviders(@Nonnull List<AbstractTreeNode> selectedNodes, @Nonnull Key dataId) {
    List<TreeStructureProvider> providers = getProvidersDumbAware();
    if (!providers.isEmpty()) {
      for (TreeStructureProvider treeStructureProvider : providers) {
        Object fromProvider = treeStructureProvider.getData(selectedNodes, dataId);
        if (fromProvider != null) {
          return fromProvider;
        }
      }
    }
    return null;
  }

  @Nonnull
  private List<TreeStructureProvider> getProvidersDumbAware() {
    if (myProject == null) {
      return Collections.emptyList();
    }

    List<TreeStructureProvider> providers = getProviders();
    if (providers == null) {
      return Collections.emptyList();
    }

    return DumbService.getInstance(myProject).filterByDumbAwareness(providers);
  }
}
