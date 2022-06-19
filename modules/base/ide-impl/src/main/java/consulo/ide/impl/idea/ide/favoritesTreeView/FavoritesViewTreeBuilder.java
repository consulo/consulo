// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.favoritesTreeView;

import consulo.language.psi.*;
import consulo.ide.impl.idea.ide.CopyPasteUtil;
import consulo.ide.impl.idea.ide.projectView.BaseProjectTreeBuilder;
import consulo.ide.impl.idea.ide.projectView.ProjectViewPsiTreeChangeListener;
import consulo.ide.impl.idea.ide.projectView.impl.ProjectAbstractTreeStructureBase;
import consulo.project.ui.view.tree.AbstractTreeNode;
import consulo.ui.ex.tree.AbstractTreeStructure;
import consulo.ui.ex.awt.tree.AbstractTreeUpdater;
import consulo.ui.ex.tree.NodeDescriptor;
import consulo.language.internal.InternalStdFileTypes;
import consulo.project.Project;
import consulo.module.content.layer.event.ModuleRootEvent;
import consulo.module.content.layer.event.ModuleRootListener;
import consulo.util.concurrent.ActionCallback;
import consulo.ide.impl.idea.openapi.util.Comparing;
import consulo.virtualFileSystem.status.FileStatusListener;
import consulo.virtualFileSystem.status.FileStatusManager;
import consulo.virtualFileSystem.VirtualFile;
import consulo.component.messagebus.MessageBusConnection;
import consulo.ui.ex.awt.tree.TreeUtil;
import consulo.util.concurrent.Promise;
import consulo.util.concurrent.Promises;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

/**
 * @author Konstantin Bulenkov
 */
public class FavoritesViewTreeBuilder extends BaseProjectTreeBuilder {
  public static final String ID = "Favorites";

  public FavoritesViewTreeBuilder(@Nonnull Project project, JTree tree, DefaultTreeModel treeModel, ProjectAbstractTreeStructureBase treeStructure) {
    super(project, tree, treeModel, treeStructure, new FavoriteComparator());
    final MessageBusConnection bus = myProject.getMessageBus().connect(this);
    ProjectViewPsiTreeChangeListener psiTreeChangeListener = new ProjectViewPsiTreeChangeListener(myProject) {
      @Override
      protected DefaultMutableTreeNode getRootNode() {
        return FavoritesViewTreeBuilder.this.getRootNode();
      }

      @Override
      protected AbstractTreeUpdater getUpdater() {
        return FavoritesViewTreeBuilder.this.getUpdater();
      }

      @Override
      protected boolean isFlattenPackages() {
        return getStructure().isFlattenPackages();
      }

      @Override
      protected void childrenChanged(PsiElement parent, final boolean stopProcessingForThisModificationCount) {
        PsiElement containingFile = parent instanceof PsiDirectory ? parent : parent.getContainingFile();
        if (containingFile != null && findNodeByElement(containingFile) == null) {
          queueUpdate(true);
        }
        else {
          super.childrenChanged(parent, true);
        }
      }
    };
    bus.subscribe(ModuleRootListener.class, new ModuleRootListener() {
      @Override
      public void rootsChanged(@Nonnull ModuleRootEvent event) {
        queueUpdate(true);
      }
    });
    PsiManager.getInstance(myProject).addPsiTreeChangeListener(psiTreeChangeListener, this);
    FileStatusListener fileStatusListener = new MyFileStatusListener();
    FileStatusManager.getInstance(myProject).addFileStatusListener(fileStatusListener, this);
    CopyPasteUtil.addDefaultListener(this, this::addSubtreeToUpdateByElement);

    FavoritesListener favoritesListener = new FavoritesListener() {
      @Override
      public void rootsChanged() {
        updateFromRoot();
      }

      @Override
      public void listAdded(@Nonnull String listName) {
        updateFromRoot();
      }

      @Override
      public void listRemoved(@Nonnull String listName) {
        updateFromRoot();
      }
    };
    initRootNode();
    FavoritesManager.getInstance(myProject).addFavoritesListener(favoritesListener, this);
  }

  @Nonnull
  public FavoritesTreeStructure getStructure() {
    final AbstractTreeStructure structure = getTreeStructure();
    assert structure instanceof FavoritesTreeStructure;
    return (FavoritesTreeStructure)structure;
  }

  public AbstractTreeNode getRoot() {
    final Object rootElement = getRootElement();
    assert rootElement instanceof AbstractTreeNode;
    return (AbstractTreeNode)rootElement;
  }

  @Override
  public void updateFromRoot() {
    updateFromRootCB();
  }

  @Nonnull
  public ActionCallback updateFromRootCB() {
    getStructure().rootsChanged();
    if (isDisposed()) return ActionCallback.DONE;
    getUpdater().cancelAllRequests();
    return queueUpdate();
  }

  @Nonnull
  @Override
  public Promise<Object> selectAsync(Object element, VirtualFile file, boolean requestFocus) {
    final DefaultMutableTreeNode node = findSmartFirstLevelNodeByElement(element);
    if (node != null) {
      return Promises.toPromise(TreeUtil.selectInTree(node, requestFocus, getTree()));
    }
    return super.selectAsync(element, file, requestFocus);
  }

  @Nullable
  private static DefaultMutableTreeNode findFirstLevelNodeWithObject(final DefaultMutableTreeNode aRoot, final Object aObject) {
    for (int i = 0; i < aRoot.getChildCount(); i++) {
      final DefaultMutableTreeNode child = (DefaultMutableTreeNode)aRoot.getChildAt(i);
      Object userObject = child.getUserObject();
      if (userObject instanceof FavoritesTreeNodeDescriptor) {
        if (Comparing.equal(((FavoritesTreeNodeDescriptor)userObject).getElement(), aObject)) {
          return child;
        }
      }
    }
    return null;
  }

  @Override
  protected Object findNodeByElement(@Nonnull Object element) {
    final Object node = findSmartFirstLevelNodeByElement(element);
    if (node != null) return node;
    return super.findNodeByElement(element);
  }

  @Nullable
  DefaultMutableTreeNode findSmartFirstLevelNodeByElement(final Object element) {
    for (Object child : getRoot().getChildren()) {
      AbstractTreeNode favorite = (AbstractTreeNode)child;
      Object currentValue = favorite.getValue();
      if (currentValue instanceof SmartPsiElementPointer) {
        currentValue = ((SmartPsiElementPointer)favorite.getValue()).getElement();
      }
       /*else if (currentValue instanceof PsiJavaFile) {
        final PsiClass[] classes = ((PsiJavaFile)currentValue).getClasses();
        if (classes.length > 0) {
          currentValue = classes[0];
        }
      }*/
      if (Comparing.equal(element, currentValue)) {
        final DefaultMutableTreeNode nodeWithObject = findFirstLevelNodeWithObject((DefaultMutableTreeNode)getTree().getModel().getRoot(), favorite);
        if (nodeWithObject != null) {
          return nodeWithObject;
        }
      }
    }
    return null;
  }

  @Override
  protected boolean isAlwaysShowPlus(NodeDescriptor nodeDescriptor) {
    final Object[] childElements = getStructure().getChildElements(nodeDescriptor);
    return childElements != null && childElements.length > 0;
  }

  @Override
  protected boolean isAutoExpandNode(NodeDescriptor nodeDescriptor) {
    return nodeDescriptor.getParentDescriptor() == null;
  }

  private final class MyFileStatusListener implements FileStatusListener {
    @Override
    public void fileStatusesChanged() {
      queueUpdateFrom(getRootNode(), false);
    }

    @Override
    public void fileStatusChanged(@Nonnull VirtualFile vFile) {
      PsiElement element = PsiUtilCore.findFileSystemItem(myProject, vFile);
      if (element != null && !addSubtreeToUpdateByElement(element) && element instanceof PsiFile && ((PsiFile)element).getFileType() == InternalStdFileTypes.JAVA) {
        addSubtreeToUpdateByElement(((PsiFile)element).getContainingDirectory());
      }
    }
  }
}

