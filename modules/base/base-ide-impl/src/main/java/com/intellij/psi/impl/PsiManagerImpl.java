// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.impl;

import com.intellij.lang.PsiBuilderFactory;
import consulo.disposer.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.WriteAction;
import consulo.logging.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressIndicatorProvider;
import com.intellij.openapi.progress.util.AbstractProgressIndicatorExBase;
import com.intellij.openapi.progress.util.ProgressWrapper;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.FileIndexFacade;
import consulo.disposer.Disposer;
import com.intellij.openapi.vfs.NonPhysicalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileFilter;
import com.intellij.openapi.wm.ex.ProgressIndicatorEx;
import com.intellij.psi.*;
import com.intellij.psi.impl.file.impl.FileManager;
import com.intellij.psi.impl.file.impl.FileManagerImpl;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.messages.Topic;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.jetbrains.annotations.TestOnly;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Singleton
public final class PsiManagerImpl extends PsiManagerEx {
  private static final Logger LOG = Logger.getInstance(PsiManagerImpl.class);

  private final Project myProject;
  private final Provider<FileIndexFacade> myFileIndex;
  private final PsiModificationTracker myModificationTracker;

  private final FileManagerImpl myFileManager;

  private final List<PsiTreeChangePreprocessor> myTreeChangePreprocessors = ContainerUtil.createLockFreeCopyOnWriteList();
  private final List<PsiTreeChangeListener> myTreeChangeListeners = ContainerUtil.createLockFreeCopyOnWriteList();
  private boolean myTreeChangeEventIsFiring;

  private boolean myIsDisposed;

  private VirtualFileFilter myAssertOnFileLoadingFilter = VirtualFileFilter.NONE;

  private final AtomicInteger myBatchFilesProcessingModeCount = new AtomicInteger(0);

  public static final Topic<AnyPsiChangeListener> ANY_PSI_CHANGE_TOPIC = Topic.create("ANY_PSI_CHANGE_TOPIC", AnyPsiChangeListener.class, Topic.BroadcastDirection.TO_PARENT);

  @Inject
  public PsiManagerImpl(@Nonnull Project project, @Nonnull Provider<FileIndexFacade> fileIndexFacadeProvider, @Nonnull PsiModificationTracker modificationTracker) {
    // we need to initialize PsiBuilderFactory service so it won't initialize under PsiLock from ChameleonTransform
    PsiBuilderFactory.getInstance();

    myProject = project;
    myFileIndex = fileIndexFacadeProvider;
    myModificationTracker = modificationTracker;

    myFileManager = new FileManagerImpl(this, fileIndexFacadeProvider);

    myTreeChangePreprocessors.add((PsiTreeChangePreprocessor)myModificationTracker);

    Disposer.register(project, () -> myIsDisposed = true);
  }

  @Override
  public boolean isDisposed() {
    return myIsDisposed;
  }

  @Override
  public void dropResolveCaches() {
    myFileManager.processQueue();
    beforeChange(true);
  }

  @Override
  public void dropPsiCaches() {
    dropResolveCaches();
    WriteAction.run(myFileManager::firePropertyChangedForUnloadedPsi);
  }

  @Override
  public boolean isInProject(@Nonnull PsiElement element) {
    if (element instanceof PsiDirectoryContainer) {
      PsiDirectory[] dirs = ((PsiDirectoryContainer)element).getDirectories();
      for (PsiDirectory dir : dirs) {
        if (!isInProject(dir)) return false;
      }
      return true;
    }

    PsiFile file = element.getContainingFile();
    VirtualFile virtualFile = null;
    if (file != null) {
      virtualFile = file.getViewProvider().getVirtualFile();
    }
    else if (element instanceof PsiFileSystemItem) {
      virtualFile = ((PsiFileSystemItem)element).getVirtualFile();
    }
    if (file != null && file.isPhysical() && virtualFile.getFileSystem() instanceof NonPhysicalFileSystem) return true;

    return virtualFile != null && myFileIndex.get().isInContent(virtualFile);
  }

  @Override
  @TestOnly
  public void setAssertOnFileLoadingFilter(@Nonnull VirtualFileFilter filter, @Nonnull Disposable parentDisposable) {
    // Find something to ensure there's no changed files waiting to be processed in repository indices.
    myAssertOnFileLoadingFilter = filter;
    Disposer.register(parentDisposable, () -> myAssertOnFileLoadingFilter = VirtualFileFilter.NONE);
  }

  @Override
  public boolean isAssertOnFileLoading(@Nonnull VirtualFile file) {
    return myAssertOnFileLoadingFilter.accept(file);
  }

  @Override
  @Nonnull
  public Project getProject() {
    return myProject;
  }

  @Override
  @Nonnull
  public FileManager getFileManager() {
    return myFileManager;
  }

  @Override
  public boolean areElementsEquivalent(PsiElement element1, PsiElement element2) {
    ProgressIndicatorProvider.checkCanceled(); // We hope this method is being called often enough to cancel daemon processes smoothly

    if (element1 == element2) return true;
    if (element1 == null || element2 == null) {
      return false;
    }

    return element1.equals(element2) || element1.isEquivalentTo(element2) || element2.isEquivalentTo(element1);
  }

  @Override
  public PsiFile findFile(@Nonnull VirtualFile file) {
    ProgressIndicatorProvider.checkCanceled();
    return myFileManager.findFile(file);
  }

  @Nonnull
  @Override
  public FileViewProvider findViewProvider(@Nonnull VirtualFile file) {
    ProgressIndicatorProvider.checkCanceled();
    return myFileManager.findViewProvider(file);
  }

  @Override
  public PsiDirectory findDirectory(@Nonnull VirtualFile file) {
    ProgressIndicatorProvider.checkCanceled();
    return myFileManager.findDirectory(file);
  }

  @Override
  public void reloadFromDisk(@Nonnull PsiFile file) {
    myFileManager.reloadFromDisk(file);
  }

  @Override
  public void addPsiTreeChangeListener(@Nonnull PsiTreeChangeListener listener) {
    myTreeChangeListeners.add(listener);
  }

  @Override
  public void addPsiTreeChangeListener(@Nonnull final PsiTreeChangeListener listener, @Nonnull Disposable parentDisposable) {
    addPsiTreeChangeListener(listener);
    Disposer.register(parentDisposable, () -> removePsiTreeChangeListener(listener));
  }

  @Override
  public void removePsiTreeChangeListener(@Nonnull PsiTreeChangeListener listener) {
    myTreeChangeListeners.remove(listener);
  }

  private static String logPsi(@Nullable PsiElement element) {
    return element == null ? " null" : element.getClass().getName();
  }

  @Override
  public void beforeChildAddition(@Nonnull PsiTreeChangeEventImpl event) {
    beforeChange(true);
    event.setCode(PsiTreeChangeEventImpl.PsiEventType.BEFORE_CHILD_ADDITION);
    if (LOG.isDebugEnabled()) {
      LOG.debug("beforeChildAddition: event = " + event);
    }
    fireEvent(event);
  }

  @Override
  public void beforeChildRemoval(@Nonnull PsiTreeChangeEventImpl event) {
    beforeChange(true);
    event.setCode(PsiTreeChangeEventImpl.PsiEventType.BEFORE_CHILD_REMOVAL);
    if (LOG.isDebugEnabled()) {
      LOG.debug("beforeChildRemoval: child = " + logPsi(event.getChild()) + ", parent = " + logPsi(event.getParent()));
    }
    fireEvent(event);
  }

  @Override
  public void beforeChildReplacement(@Nonnull PsiTreeChangeEventImpl event) {
    beforeChange(true);
    event.setCode(PsiTreeChangeEventImpl.PsiEventType.BEFORE_CHILD_REPLACEMENT);
    if (LOG.isDebugEnabled()) {
      LOG.debug("beforeChildReplacement: oldChild = " + logPsi(event.getOldChild()));
    }
    fireEvent(event);
  }

  public void beforeChildrenChange(@Nonnull PsiTreeChangeEventImpl event) {
    beforeChange(true);
    event.setCode(PsiTreeChangeEventImpl.PsiEventType.BEFORE_CHILDREN_CHANGE);
    if (LOG.isDebugEnabled()) {
      LOG.debug("beforeChildrenChange: parent = " + logPsi(event.getParent()));
    }
    fireEvent(event);
  }

  public void beforeChildMovement(@Nonnull PsiTreeChangeEventImpl event) {
    beforeChange(true);
    event.setCode(PsiTreeChangeEventImpl.PsiEventType.BEFORE_CHILD_MOVEMENT);
    if (LOG.isDebugEnabled()) {
      LOG.debug("beforeChildMovement: child = " + logPsi(event.getChild()) + ", oldParent = " + logPsi(event.getOldParent()) + ", newParent = " + logPsi(event.getNewParent()));
    }
    fireEvent(event);
  }

  public void beforePropertyChange(@Nonnull PsiTreeChangeEventImpl event) {
    beforeChange(true);
    event.setCode(PsiTreeChangeEventImpl.PsiEventType.BEFORE_PROPERTY_CHANGE);
    if (LOG.isDebugEnabled()) {
      LOG.debug("beforePropertyChange: element = " + logPsi(event.getElement()) + ", propertyName = " + event.getPropertyName() + ", oldValue = " + arrayToString(event.getOldValue()));
    }
    fireEvent(event);
  }

  private static Object arrayToString(Object value) {
    return value instanceof Object[] ? Arrays.deepToString((Object[])value) : value;
  }

  public void childAdded(@Nonnull PsiTreeChangeEventImpl event) {
    event.setCode(PsiTreeChangeEventImpl.PsiEventType.CHILD_ADDED);
    if (LOG.isDebugEnabled()) {
      LOG.debug("childAdded: child = " + logPsi(event.getChild()) + ", parent = " + logPsi(event.getParent()));
    }
    fireEvent(event);
    afterChange(true);
  }

  public void childRemoved(@Nonnull PsiTreeChangeEventImpl event) {
    event.setCode(PsiTreeChangeEventImpl.PsiEventType.CHILD_REMOVED);
    if (LOG.isDebugEnabled()) {
      LOG.debug("childRemoved: child = " + logPsi(event.getChild()) + ", parent = " + logPsi(event.getParent()));
    }
    fireEvent(event);
    afterChange(true);
  }

  public void childReplaced(@Nonnull PsiTreeChangeEventImpl event) {
    event.setCode(PsiTreeChangeEventImpl.PsiEventType.CHILD_REPLACED);
    if (LOG.isDebugEnabled()) {
      LOG.debug("childReplaced: oldChild = " + logPsi(event.getOldChild()) + ", newChild = " + logPsi(event.getNewChild()) + ", parent = " + logPsi(event.getParent()));
    }
    fireEvent(event);
    afterChange(true);
  }

  public void childMoved(@Nonnull PsiTreeChangeEventImpl event) {
    event.setCode(PsiTreeChangeEventImpl.PsiEventType.CHILD_MOVED);
    if (LOG.isDebugEnabled()) {
      LOG.debug("childMoved: child = " + logPsi(event.getChild()) + ", oldParent = " + logPsi(event.getOldParent()) + ", newParent = " + logPsi(event.getNewParent()));
    }
    fireEvent(event);
    afterChange(true);
  }

  public void childrenChanged(@Nonnull PsiTreeChangeEventImpl event) {
    event.setCode(PsiTreeChangeEventImpl.PsiEventType.CHILDREN_CHANGED);
    if (LOG.isDebugEnabled()) {
      LOG.debug("childrenChanged: parent = " + logPsi(event.getParent()));
    }
    fireEvent(event);
    afterChange(true);
  }

  public void propertyChanged(@Nonnull PsiTreeChangeEventImpl event) {
    event.setCode(PsiTreeChangeEventImpl.PsiEventType.PROPERTY_CHANGED);
    if (LOG.isDebugEnabled()) {
      LOG.debug("propertyChanged: element = " +
                logPsi(event.getElement()) +
                ", propertyName = " +
                event.getPropertyName() +
                ", oldValue = " +
                arrayToString(event.getOldValue()) +
                ", newValue = " +
                arrayToString(event.getNewValue()));
    }
    fireEvent(event);
    afterChange(true);
  }

  public void addTreeChangePreprocessor(@Nonnull PsiTreeChangePreprocessor preprocessor) {
    myTreeChangePreprocessors.add(preprocessor);
  }

  public void removeTreeChangePreprocessor(@Nonnull PsiTreeChangePreprocessor preprocessor) {
    myTreeChangePreprocessors.remove(preprocessor);
  }

  private void fireEvent(@Nonnull PsiTreeChangeEventImpl event) {
    boolean isRealTreeChange = event.getCode() != PsiTreeChangeEventImpl.PsiEventType.PROPERTY_CHANGED && event.getCode() != PsiTreeChangeEventImpl.PsiEventType.BEFORE_PROPERTY_CHANGE;

    PsiFile file = event.getFile();
    if (file == null || file.isPhysical()) {
      ApplicationManager.getApplication().assertWriteAccessAllowed();
    }
    if (isRealTreeChange) {
      LOG.assertTrue(!myTreeChangeEventIsFiring, "Changes to PSI are not allowed inside event processing");
      myTreeChangeEventIsFiring = true;
    }
    try {
      for (PsiTreeChangePreprocessor preprocessor : myTreeChangePreprocessors) {
        preprocessor.treeChanged(event);
      }
      for (PsiTreeChangePreprocessor preprocessor : PsiTreeChangePreprocessor.EP_NAME.getExtensionList(myProject)) {
        try {
          preprocessor.treeChanged(event);
        }
        catch (Throwable e) {
          LOG.error(e);
        }
      }
      for (PsiTreeChangeListener listener : myTreeChangeListeners) {
        try {
          switch (event.getCode()) {
            case BEFORE_CHILD_ADDITION:
              listener.beforeChildAddition(event);
              break;

            case BEFORE_CHILD_REMOVAL:
              listener.beforeChildRemoval(event);
              break;

            case BEFORE_CHILD_REPLACEMENT:
              listener.beforeChildReplacement(event);
              break;

            case BEFORE_CHILD_MOVEMENT:
              listener.beforeChildMovement(event);
              break;

            case BEFORE_CHILDREN_CHANGE:
              listener.beforeChildrenChange(event);
              break;

            case BEFORE_PROPERTY_CHANGE:
              listener.beforePropertyChange(event);
              break;

            case CHILD_ADDED:
              listener.childAdded(event);
              break;

            case CHILD_REMOVED:
              listener.childRemoved(event);
              break;

            case CHILD_REPLACED:
              listener.childReplaced(event);
              break;

            case CHILD_MOVED:
              listener.childMoved(event);
              break;

            case CHILDREN_CHANGED:
              listener.childrenChanged(event);
              break;

            case PROPERTY_CHANGED:
              listener.propertyChanged(event);
              break;
          }
        }
        catch (Throwable e) {
          LOG.error(e);
        }
      }
    }
    finally {
      if (isRealTreeChange) {
        myTreeChangeEventIsFiring = false;
      }
    }
  }

  @Override
  public void registerRunnableToRunOnChange(@Nonnull final Runnable runnable) {
    myProject.getMessageBus().connect().subscribe(ANY_PSI_CHANGE_TOPIC, new AnyPsiChangeListener() {
      @Override
      public void beforePsiChanged(boolean isPhysical) {
        if (isPhysical) runnable.run();
      }
    });
  }

  @Override
  public void registerRunnableToRunOnAnyChange(@Nonnull final Runnable runnable) { // includes non-physical changes
    myProject.getMessageBus().connect().subscribe(ANY_PSI_CHANGE_TOPIC, new AnyPsiChangeListener() {
      @Override
      public void beforePsiChanged(boolean isPhysical) {
        runnable.run();
      }
    });
  }

  @Override
  public void registerRunnableToRunAfterAnyChange(@Nonnull final Runnable runnable) { // includes non-physical changes
    myProject.getMessageBus().connect().subscribe(ANY_PSI_CHANGE_TOPIC, new AnyPsiChangeListener() {
      @Override
      public void afterPsiChanged(boolean isPhysical) {
        runnable.run();
      }
    });
  }

  @Override
  public void beforeChange(boolean isPhysical) {
    myProject.getMessageBus().syncPublisher(ANY_PSI_CHANGE_TOPIC).beforePsiChanged(isPhysical);
  }

  @Override
  public void afterChange(boolean isPhysical) {
    myProject.getMessageBus().syncPublisher(ANY_PSI_CHANGE_TOPIC).afterPsiChanged(isPhysical);
  }

  @Override
  @Nonnull
  public PsiModificationTracker getModificationTracker() {
    return myModificationTracker;
  }

  @Override
  public void startBatchFilesProcessingMode() {
    myBatchFilesProcessingModeCount.incrementAndGet();
  }

  @Override
  public void finishBatchFilesProcessingMode() {
    myBatchFilesProcessingModeCount.decrementAndGet();
    LOG.assertTrue(myBatchFilesProcessingModeCount.get() >= 0);
  }

  @Override
  public boolean isBatchFilesProcessingMode() {
    return myBatchFilesProcessingModeCount.get() > 0;
  }

  @TestOnly
  public void cleanupForNextTest() {
    assert ApplicationManager.getApplication().isUnitTestMode();
    myFileManager.cleanupForNextTest();
    dropPsiCaches();
  }

  public void dropResolveCacheRegularly(@Nonnull ProgressIndicator indicator) {
    indicator = ProgressWrapper.unwrap(indicator);
    if (indicator instanceof ProgressIndicatorEx) {
      ((ProgressIndicatorEx)indicator).addStateDelegate(new AbstractProgressIndicatorExBase() {
        private final AtomicLong lastClearedTimeStamp = new AtomicLong();

        @Override
        public void setFraction(double fraction) {
          long current = System.currentTimeMillis();
          long last = lastClearedTimeStamp.get();
          if (current - last >= 500 && lastClearedTimeStamp.compareAndSet(last, current)) {
            // fraction is changed when each file is processed =>
            // resolve caches used when searching in that file are likely to be not needed anymore
            dropResolveCaches();
          }
        }
      });
    }
  }
}
