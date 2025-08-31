/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

package consulo.ide.impl.idea.ide.projectView.impl;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.impl.internal.IdeaModalityState;
import consulo.component.messagebus.MessageBusConnection;
import consulo.ide.impl.idea.ide.CopyPasteUtil;
import consulo.bookmark.Bookmark;
import consulo.bookmark.event.BookmarksListener;
import consulo.ide.impl.idea.ide.projectView.BaseProjectTreeBuilder;
import consulo.ide.impl.idea.ide.projectView.ProjectViewPsiTreeChangeListener;
import consulo.language.editor.wolfAnalyzer.ProblemListener;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiManager;
import consulo.module.content.layer.event.ModuleRootEvent;
import consulo.module.content.layer.event.ModuleRootListener;
import consulo.project.Project;
import consulo.project.ui.view.tree.ProjectViewNode;
import consulo.ui.ex.awt.CopyPasteManager;
import consulo.ui.ex.awt.tree.AbstractTreeUpdater;
import consulo.ui.ex.awt.util.Alarm;
import consulo.ui.ex.tree.AbstractTreeStructure;
import consulo.ui.ex.tree.NodeDescriptor;
import consulo.util.collection.SmartList;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.status.FileStatusListener;
import consulo.virtualFileSystem.status.FileStatusManager;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.util.*;

public class ProjectTreeBuilder extends BaseProjectTreeBuilder {
  public ProjectTreeBuilder(@Nonnull Project project,
                            @Nonnull JTree tree,
                            @Nonnull DefaultTreeModel treeModel,
                            @Nullable Comparator<NodeDescriptor> comparator,
                            @Nonnull ProjectAbstractTreeStructureBase treeStructure) {
    super(project, tree, treeModel, treeStructure, comparator);

    MessageBusConnection connection = project.getMessageBus().connect(this);

    connection.subscribe(ModuleRootListener.class, new ModuleRootListener() {
      @Override
      public void rootsChanged(ModuleRootEvent event) {
        queueUpdate();
      }
    });

    connection.subscribe(BookmarksListener.class, new MyBookmarksListener());

    PsiManager.getInstance(project).addPsiTreeChangeListener(createPsiTreeChangeListener(project), this);
    FileStatusManager.getInstance(project).addFileStatusListener(new MyFileStatusListener(), this);
    CopyPasteManager.getInstance().addContentChangedListener(new CopyPasteUtil.DefaultCopyPasteListener(getUpdater()), this);

    connection.subscribe(ProblemListener.class, new MyProblemListener());

    setCanYieldUpdate(true);

    initRootNode();
  }

  /**
   * Creates psi tree changes listener. This method will be invoked in constructor of ProjectTreeBuilder
   * thus builder object will be not completely initialized
   * @param project Project
   * @return Listener
   */
  protected ProjectViewPsiTreeChangeListener createPsiTreeChangeListener(Project project) {
    return new ProjectTreeBuilderPsiListener(project);
  }

  protected class ProjectTreeBuilderPsiListener extends ProjectViewPsiTreeChangeListener {
    public ProjectTreeBuilderPsiListener(Project project) {
      super(project);
    }

    @Override
    protected DefaultMutableTreeNode getRootNode(){
      return ProjectTreeBuilder.this.getRootNode();
    }

    @Override
    protected AbstractTreeUpdater getUpdater() {
      return ProjectTreeBuilder.this.getUpdater();
    }

    @Override
    protected boolean isFlattenPackages(){
      AbstractTreeStructure structure = getTreeStructure();
      return structure instanceof AbstractProjectTreeStructure && ((AbstractProjectTreeStructure)structure).isFlattenPackages();
    }
  }

  private final class MyBookmarksListener implements BookmarksListener {
    @Override
    public void bookmarkAdded(@Nonnull Bookmark b) {
      updateForFile(b.getFile());
    }

    @Override
    public void bookmarkRemoved(@Nonnull Bookmark b) {
      updateForFile(b.getFile());
    }

    @Override
    public void bookmarkChanged(@Nonnull Bookmark b) {
      updateForFile(b.getFile());
    }

    private void updateForFile(@Nonnull VirtualFile file) {
      PsiElement element = findPsi(file);
      if (element != null) {
        queueUpdateFrom(element, false);
      }
    }
  }

  private final class MyFileStatusListener implements FileStatusListener {
    @Override
    public void fileStatusesChanged() {
      queueUpdate(false);
    }

    @Override
    public void fileStatusChanged(@Nonnull VirtualFile vFile) {
      queueUpdate(false);
    }
  }

  @RequiredReadAction
  private PsiElement findPsi(@Nonnull VirtualFile vFile) {
    if (!vFile.isValid()) return null;
    PsiManager psiManager = PsiManager.getInstance(myProject);
    return vFile.isDirectory() ? psiManager.findDirectory(vFile) : psiManager.findFile(vFile);
  }

  private class MyProblemListener implements ProblemListener {
    private final Alarm myUpdateProblemAlarm = new Alarm();
    private final Collection<VirtualFile> myFilesToRefresh = new HashSet<>();

    @Override
    public void problemsAppeared(@Nonnull VirtualFile file) {
      queueUpdate(file);
    }

    @Override
    public void problemsDisappeared(@Nonnull VirtualFile file) {
      queueUpdate(file);
    }

    private void queueUpdate(@Nonnull VirtualFile fileToRefresh) {
      synchronized (myFilesToRefresh) {
        if (myFilesToRefresh.add(fileToRefresh)) {
          myUpdateProblemAlarm.cancelAllRequests();
          myUpdateProblemAlarm.addRequest(() -> {
            if (!myProject.isOpen()) return;
            Set<VirtualFile> filesToRefresh;
            synchronized (myFilesToRefresh) {
              filesToRefresh = new HashSet<>(myFilesToRefresh);
            }
            DefaultMutableTreeNode rootNode = getRootNode();
            if (rootNode != null) {
              updateNodesContaining(filesToRefresh, rootNode);
            }
            synchronized (myFilesToRefresh) {
              myFilesToRefresh.removeAll(filesToRefresh);
            }
          }, 200, IdeaModalityState.nonModal());
        }
      }
    }
  }

  private void updateNodesContaining(@Nonnull Collection<VirtualFile> filesToRefresh, @Nonnull DefaultMutableTreeNode rootNode) {
    if (!(rootNode.getUserObject() instanceof ProjectViewNode)) return;
    ProjectViewNode node = (ProjectViewNode)rootNode.getUserObject();
    Collection<VirtualFile> containingFiles = null;
    for (VirtualFile virtualFile : filesToRefresh) {
      if (!virtualFile.isValid()) {
        addSubtreeToUpdate(rootNode); // file must be deleted
        return;
      }
      if (node.contains(virtualFile)) {
        if (containingFiles == null) containingFiles = new SmartList<>();
        containingFiles.add(virtualFile);
      }
    }
    if (containingFiles != null) {
      updateNode(rootNode);
      Enumeration children = rootNode.children();
      while (children.hasMoreElements()) {
        DefaultMutableTreeNode child = (DefaultMutableTreeNode)children.nextElement();
        updateNodesContaining(containingFiles, child);
      }
    }
  }
}
