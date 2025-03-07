/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ide.favoritesTreeView;

import consulo.bookmark.ui.view.FavoritesListNode;
import consulo.bookmark.ui.view.FavoritesTreeNodeDescriptor;
import consulo.disposer.Disposer;
import consulo.ide.impl.idea.ide.dnd.FileCopyPasteUtil;
import consulo.ide.impl.idea.ide.dnd.TransferableWrapper;
import consulo.ui.ex.awt.dnd.DnDAwareTree;
import consulo.language.psi.PsiFileSystemItem;
import consulo.language.psi.PsiManager;
import consulo.language.psi.SmartPsiElementPointer;
import consulo.project.Project;
import consulo.project.ui.view.tree.AbstractTreeNode;
import consulo.ui.ex.awt.RelativeRectangle;
import consulo.ui.ex.awt.dnd.*;
import consulo.ui.ex.awt.tree.AbstractTreeBuilder;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nullable;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Konstantin Bulenkov
 */
public class FavoritesPanel {
  private Project myProject;
  private FavoritesTreeViewPanel myViewPanel;
  private DnDAwareTree myTree;
  private AbstractTreeBuilder myTreeBuilder;

  public FavoritesPanel(Project project) {
    myProject = project;
    myViewPanel = new FavoritesTreeViewPanel(myProject);
    myTree = myViewPanel.getTree();
    myTreeBuilder = myViewPanel.getBuilder();
    if (myTreeBuilder != null) {
      Disposer.register(myProject, myTreeBuilder);
    }
    setupDnD();
  }

  public FavoritesTreeViewPanel getPanel() {
    return myViewPanel;
  }

  private void setupDnD() {
    DnDSupport.createBuilder(myTree).setBeanProvider(info -> {
      final TreePath path = myTree.getPathForLocation(info.getPoint().x, info.getPoint().y);
      if (path != null) {
        return new DnDDragStartBean(path);
      }
      return new DnDDragStartBean("");
    })
            // todo process drag-and-drop here for tasks
            .setTargetChecker(new DnDTargetChecker() {
              @Override
              public boolean update(DnDEvent event) {
                final Object obj = event.getAttachedObject();
                if ("".equals(obj)) {
                  event.setDropPossible(false);
                  return false;
                }
                if (obj instanceof TreePath && ((TreePath)obj).getPathCount() <= 2) {
                  event.setDropPossible(false);
                  return true;
                }
                FavoritesListNode node = myViewPanel.findFavoritesListNode(event.getPoint());
                highlight(node, event);
                if (node != null) {
                  event.setDropPossible(true);
                  return true;
                }
                event.setDropPossible(false);
                return false;
              }
            }).setDropHandler(new DnDDropHandler() {
      @Override
      public void drop(DnDEvent event) {
        final FavoritesListNode node = myViewPanel.findFavoritesListNode(event.getPoint());
        final FavoritesManagerImpl mgr = FavoritesManagerImpl.getInstance(myProject);

        if (node == null) return;

        final String listTo = node.getValue();
        final Object obj = event.getAttachedObject();

        if (obj instanceof TreePath) {
          final TreePath path = (TreePath)obj;
          final String listFrom = FavoritesTreeViewPanel.getListNodeFromPath(path).getValue();
          if (listTo.equals(listFrom)) return;
          if (path.getPathCount() == 3) {
            final AbstractTreeNode abstractTreeNode = ((FavoritesTreeNodeDescriptor)((DefaultMutableTreeNode)path.getLastPathComponent()).getUserObject()).getElement();
            Object element = abstractTreeNode.getValue();
            mgr.removeRoot(listFrom, Collections.singletonList(abstractTreeNode));
            if (element instanceof SmartPsiElementPointer) {
              element = ((SmartPsiElementPointer)element).getElement();
            }
            mgr.addRoots(listTo, null, element);
          }
        }
        else if (obj instanceof TransferableWrapper) {
          myViewPanel.dropPsiElements(mgr, node, ((TransferableWrapper)obj).getPsiElements());
        }
        else if (obj instanceof DnDNativeTarget.EventInfo) {
          myViewPanel.dropPsiElements(mgr, node, getPsiFiles(FileCopyPasteUtil.getFileList(((DnDNativeTarget.EventInfo)obj).getTransferable())));
        }
      }
    }).enableAsNativeTarget().setDisposableParent(myProject).install();
  }

  private void highlight(FavoritesListNode node, DnDEvent event) {
    if (node != null) {
      TreePath pathToList = myTree.getPath(node);
      while (pathToList != null) {
        final Object pathObj = pathToList.getLastPathComponent();
        if (pathObj instanceof DefaultMutableTreeNode) {
          final Object userObject = ((DefaultMutableTreeNode)pathObj).getUserObject();
          if (userObject instanceof FavoritesTreeNodeDescriptor) {
            if (((FavoritesTreeNodeDescriptor)userObject).getElement() == node) {
              break;
            }
          }
        }
        pathToList = pathToList.getParentPath();
      }
      if (pathToList != null) {
        Rectangle bounds = myTree.getPathBounds(pathToList);
        if (bounds != null) {
          event.setHighlighting(new RelativeRectangle(myTree, bounds), DnDEvent.DropTargetHighlightingType.RECTANGLE);
        }
      }
    }
    else {
      event.hideHighlighter();
    }
  }

  @Nullable
  protected PsiFileSystemItem[] getPsiFiles(@Nullable List<File> fileList) {
    if (fileList == null) return null;
    List<PsiFileSystemItem> sourceFiles = new ArrayList<PsiFileSystemItem>();
    for (File file : fileList) {
      final VirtualFile vFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file);
      if (vFile != null) {
        final PsiFileSystemItem psiFile = vFile.isDirectory() ? PsiManager.getInstance(myProject).findDirectory(vFile) : PsiManager.getInstance(myProject).findFile(vFile);
        if (psiFile != null) {
          sourceFiles.add(psiFile);
        }
      }
    }
    return sourceFiles.toArray(new PsiFileSystemItem[sourceFiles.size()]);
  }
}
