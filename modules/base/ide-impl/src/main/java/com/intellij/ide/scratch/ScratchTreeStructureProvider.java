// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.scratch;

import consulo.application.AllIcons;
import com.intellij.ide.projectView.*;
import com.intellij.ide.projectView.impl.AbstractProjectViewPane;
import com.intellij.ide.projectView.impl.ProjectViewPane;
import com.intellij.ide.projectView.impl.nodes.ProjectViewProjectNode;
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode;
import com.intellij.ide.projectView.impl.nodes.PsiFileNode;
import com.intellij.ide.projectView.impl.nodes.PsiFileSystemItemFilter;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import consulo.ui.ex.tree.AbstractTreeUi;
import consulo.language.Language;
import com.intellij.openapi.actionSystem.LangDataKeys;
import consulo.application.ApplicationManager;
import consulo.application.ReadAction;
import consulo.language.file.LanguageFileType;
import consulo.application.progress.ProgressManager;
import consulo.language.psi.*;
import consulo.application.dumb.DumbAware;
import consulo.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.io.FileUtil;
import consulo.ui.ex.tree.PresentationData;
import consulo.virtualFileSystem.event.AsyncFileListener;
import com.intellij.openapi.vfs.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import consulo.virtualFileSystem.event.VFileCopyEvent;
import consulo.virtualFileSystem.event.VFileCreateEvent;
import consulo.virtualFileSystem.event.VFileEvent;
import consulo.virtualFileSystem.event.VFileMoveEvent;
import consulo.language.psi.resolve.PsiElementProcessor;
import consulo.language.psi.PsiUtilCore;
import com.intellij.util.concurrency.NonUrgentExecutor;
import consulo.application.util.ConcurrentFactoryMap;
import com.intellij.util.containers.ContainerUtil;
import consulo.util.collection.JBIterable;
import consulo.container.boot.ContainerPathManager;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.util.dataholder.Key;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentMap;

/**
 * @author gregsh
 */
public class ScratchTreeStructureProvider implements TreeStructureProvider, DumbAware {

  public ScratchTreeStructureProvider(Project project) {
    registerUpdaters(project, project, new Runnable() {
      AbstractProjectViewPane updateTarget;

      @Override
      public void run() {
        if (updateTarget == null) {
          updateTarget = ProjectView.getInstance(project).getProjectViewPaneById(ProjectViewPane.ID);
        }
        if (updateTarget != null) updateTarget.updateFromRoot(true);
      }
    });
  }

  private static void registerUpdaters(@Nonnull Project project, @Nonnull consulo.disposer.Disposable disposable, @Nonnull Runnable onUpdate) {
    String scratchPath = FileUtil.toSystemIndependentName(FileUtil.toCanonicalPath(ContainerPathManager.get().getScratchPath()));
    VirtualFileManager.getInstance().addAsyncFileListener(events -> {
      boolean update = JBIterable.from(events).find(e -> {
        ProgressManager.checkCanceled();

        final boolean isDirectory = isDirectory(e);
        final VirtualFile parent = getNewParent(e);
        return parent != null && (ScratchUtil.isScratch(parent) || isDirectory && parent.getPath().startsWith(scratchPath));
      }) != null;

      return !update ? null : new AsyncFileListener.ChangeApplier() {
        @Override
        public void afterVfsChange() {
          onUpdate.run();
        }
      };
    }, disposable);
    ConcurrentMap<RootType, Disposable> disposables = ConcurrentFactoryMap.createMap(o -> Disposable.newDisposable(o.getDisplayName()));
    for (RootType rootType : RootType.getAllRootTypes()) {
      registerRootTypeUpdater(project, rootType, onUpdate, disposable, disposables);
    }
  }

  private static void registerRootTypeUpdater(@Nonnull Project project,
                                              @Nonnull RootType rootType,
                                              @Nonnull Runnable onUpdate,
                                              @Nonnull Disposable parentDisposable,
                                              @Nonnull Map<RootType, Disposable> disposables) {
    if (rootType.isHidden()) return;
    Disposable rootDisposable = disposables.get(rootType);
    Disposer.register(parentDisposable, rootDisposable);
    ReadAction.nonBlocking(() -> rootType.registerTreeUpdater(project, parentDisposable, onUpdate)).expireWith(parentDisposable).submit(NonUrgentExecutor.getInstance());
  }

  private static VirtualFile getNewParent(@Nonnull VFileEvent e) {
    if (e instanceof VFileMoveEvent) {
      return ((VFileMoveEvent)e).getNewParent();
    }
    else if (e instanceof VFileCopyEvent) {
      return ((VFileCopyEvent)e).getNewParent();
    }
    else if (e instanceof VFileCreateEvent) {
      return ((VFileCreateEvent)e).getParent();
    }
    else {
      return Objects.requireNonNull(e.getFile()).getParent();
    }
  }

  private static boolean isDirectory(@Nonnull VFileEvent e) {
    if (e instanceof VFileCreateEvent) {
      return ((VFileCreateEvent)e).isDirectory();
    }
    else {
      return Objects.requireNonNull(e.getFile()).isDirectory();
    }
  }

  @Nullable
  private static PsiDirectory getDirectory(@Nonnull Project project, @Nonnull RootType rootType) {
    VirtualFile virtualFile = getVirtualFile(rootType);
    return virtualFile == null ? null : PsiManager.getInstance(project).findDirectory(virtualFile);
  }

  @Nullable
  private static VirtualFile getVirtualFile(@Nonnull RootType rootType) {
    String path = ScratchFileService.getInstance().getRootPath(rootType);
    return LocalFileSystem.getInstance().findFileByPath(path);
  }

  @Nullable
  private static AbstractTreeNode<?> createRootTypeNode(@Nonnull Project project, @Nonnull RootType rootType, @Nonnull ViewSettings settings) {
    if (rootType.isHidden()) return null;
    MyRootNode node = new MyRootNode(project, rootType, settings);
    return node.isEmpty() ? null : node;
  }

  @Override
  @Nonnull
  public Collection<AbstractTreeNode> modify(@Nonnull AbstractTreeNode parent, @Nonnull Collection<AbstractTreeNode> children, ViewSettings settings) {
    Project project = parent instanceof ProjectViewProjectNode ? parent.getProject() : null;
    if (project == null) return children;
    if (ApplicationManager.getApplication().isUnitTestMode()) return children;
    if (children.isEmpty() && JBIterable.from(RootType.getAllRootTypes()).filterMap(o -> createRootTypeNode(project, o, settings)).isEmpty()) {
      return children;
    }
    List<AbstractTreeNode> list = new ArrayList<>(children.size() + 1);
    list.addAll(children);
    list.add(new MyProjectNode(project, settings));
    return list;
  }

  /**
   * @deprecated Use modify method instead
   */
  @Deprecated
  public static AbstractTreeNode<?> createRootNode(@Nonnull Project project, ViewSettings settings) {
    return new MyProjectNode(project, settings);
  }

  @Override
  @Nullable
  public Object getData(@Nonnull Collection<AbstractTreeNode> selected, @Nonnull Key<?> dataId) {
    if (LangDataKeys.PASTE_TARGET_PSI_ELEMENT == dataId) {
      AbstractTreeNode<?> single = JBIterable.from(selected).single();
      if (single instanceof MyRootNode) {
        VirtualFile file = ((MyRootNode)single).getVirtualFile();
        Project project = single.getProject();
        return file == null || project == null ? null : PsiManager.getInstance(project).findDirectory(file);
      }
    }
    return null;
  }

  private static final class MyProjectNode extends ProjectViewNode<String> {
    MyProjectNode(Project project, ViewSettings settings) {
      super(project, ScratchesNamedScope.scratchesAndConsoles(), settings);
    }

    @Override
    public boolean contains(@Nonnull VirtualFile file) {
      return ScratchUtil.isScratch(file);
    }

    @Nonnull
    @Override
    public Collection<? extends AbstractTreeNode<?>> getChildren() {
      List<AbstractTreeNode<?>> list = new ArrayList<>();
      Project project = Objects.requireNonNull(getProject());
      for (RootType rootType : RootType.getAllRootTypes()) {
        ContainerUtil.addIfNotNull(list, createRootTypeNode(project, rootType, getSettings()));
      }
      return list;
    }

    @Override
    protected void update(@Nonnull PresentationData presentation) {
      presentation.setPresentableText(getValue());
      presentation.setIcon(PlatformIconGroup.scopeScratches());
    }

    @Override
    public boolean canRepresent(Object element) {
      PsiElement item = element instanceof PsiElement ? (PsiElement)element : null;
      VirtualFile virtualFile = item == null ? null : PsiUtilCore.getVirtualFile(item);
      if (virtualFile == null) return false;
      return Objects.equals(virtualFile.getPath(), FileUtil.toSystemIndependentName(ContainerPathManager.get().getScratchPath()));
    }
  }

  private static class MyRootNode extends ProjectViewNode<RootType> implements PsiFileSystemItemFilter {
    MyRootNode(Project project, @Nonnull RootType type, ViewSettings settings) {
      super(project, type, settings);
    }

    @Nonnull
    public RootType getRootType() {
      return Objects.requireNonNull(getValue());
    }

    @Override
    public boolean contains(@Nonnull VirtualFile file) {
      return getValue().containsFile(file);
    }

    @Nullable
    @Override
    public VirtualFile getVirtualFile() {
      return ScratchTreeStructureProvider.getVirtualFile(getRootType());
    }

    @Nonnull
    @Override
    public Collection<VirtualFile> getRoots() {
      return getDefaultRootsFor(getVirtualFile());
    }

    @Nonnull
    @Override
    public Collection<? extends AbstractTreeNode> getChildren() {
      //noinspection ConstantConditions
      return getDirectoryChildrenImpl(getProject(), getDirectory(), getSettings(), this);
    }

    PsiDirectory getDirectory() {
      //noinspection ConstantConditions
      return ScratchTreeStructureProvider.getDirectory(getProject(), getValue());
    }

    @Override
    protected void update(@Nonnull PresentationData presentation) {
      presentation.setIcon(AllIcons.Nodes.Folder);
      presentation.setPresentableText(getRootType().getDisplayName());
    }

    @Override
    public boolean canRepresent(Object element) {
      return Comparing.equal(getDirectory(), element);
    }

    public boolean isEmpty() {
      VirtualFile root = getVirtualFile();
      if (root == null) return true;
      RootType rootType = getRootType();
      Project project = Objects.requireNonNull(getProject());
      for (VirtualFile f : root.getChildren()) {
        if (!rootType.isIgnored(project, f)) return false;
      }
      return true;
    }

    @Override
    public boolean shouldShow(@Nonnull PsiFileSystemItem item) {
      //noinspection ConstantConditions
      return !getRootType().isIgnored(getProject(), item.getVirtualFile());
    }

    @Nonnull
    static Collection<AbstractTreeNode> getDirectoryChildrenImpl(@Nonnull Project project, @Nullable PsiDirectory directory, @Nonnull ViewSettings settings, @Nonnull PsiFileSystemItemFilter filter) {
      final List<AbstractTreeNode> result = new ArrayList<>();
      PsiElementProcessor<PsiFileSystemItem> processor = new PsiElementProcessor<PsiFileSystemItem>() {
        @Override
        public boolean execute(@Nonnull PsiFileSystemItem element) {
          if (!filter.shouldShow(element)) {
            // skip
          }
          else if (element instanceof PsiDirectory) {
            result.add(new PsiDirectoryNode(project, (PsiDirectory)element, settings, filter) {
              @Override
              public Collection<AbstractTreeNode> getChildrenImpl() {
                //noinspection ConstantConditions
                return getDirectoryChildrenImpl(getProject(), getValue(), getSettings(), getFilter());
              }
            });
          }
          else if (element instanceof PsiFile) {
            result.add(new PsiFileNode(project, (PsiFile)element, settings) {
              @Override
              public Comparable<ExtensionSortKey> getTypeSortKey() {
                PsiFile value = getValue();
                Language language = value == null ? null : value.getLanguage();
                LanguageFileType fileType = language == null ? null : language.getAssociatedFileType();
                return fileType == null ? null : new ExtensionSortKey(fileType.getDefaultExtension());
              }
            });
          }
          return true;
        }
      };

      return AbstractTreeUi.calculateYieldingToWriteAction(() -> {
        if (directory == null || !directory.isValid()) return Collections.emptyList();
        directory.processChildren(processor);
        return result;
      });
    }
  }
}
