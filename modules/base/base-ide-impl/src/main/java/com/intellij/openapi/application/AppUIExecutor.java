// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.application;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import consulo.disposer.Disposable;
import org.jetbrains.annotations.Contract;
import javax.annotation.Nonnull;

/**
 * An executor that invokes given runnables on Swing Event Dispatch thread when all constraints of a given set are satisfied at the same time.
 * The executor is created by calling {@link #onUiThread}, the constraints are specified by chained calls. For example, to invoke
 * some action when all documents are committed and indices are available, one can use
 * {@code AppUIExecutor.onUiThread().withDocumentsCommitted(project).inSmartMode(project)}.
 */
public interface AppUIExecutor extends BaseExpirableExecutor<AppUIExecutor> {

  /**
   * Creates an executor working with the given modality state.
   *
   * @see ModalityState
   */
  @Nonnull
  static AppUIExecutor onUiThread(@Nonnull ModalityState modality) {
    return AsyncExecutionService.getService().createUIExecutor(modality);
  }

  /**
   * Creates an executor working with the default modality state.
   *
   * @see ModalityState#defaultModalityState()
   */
  @Nonnull
  static AppUIExecutor onUiThread() {
    return onUiThread(ModalityState.defaultModalityState());
  }

  /**
   * Creates a Write-thread-based executor working with the given modality state.
   *
   * @see ModalityState
   */
  @Nonnull
  static AppUIExecutor onWriteThread(@Nonnull ModalityState modality) {
    return AsyncExecutionService.getService().createWriteThreadExecutor(modality);
  }


  /**
   * Creates a Write-thread-based executor working with the default modality state.
   *
   * @see ModalityState#defaultModalityState()
   */
  @Nonnull
  static AppUIExecutor onWriteThread() {
    return onWriteThread(ModalityState.defaultModalityState());
  }

  /**
   * @return an executor that should always invoke the given runnable later. Otherwise, if {@link #execute} is called
   * on dispatch thread already, and all others constraints are met, the runnable would be executed immediately.
   */
  @Nonnull
  @Contract(pure = true)
  AppUIExecutor later();

  /**
   * @return an executor that invokes runnables only when all documents are committed. Automatically expires when the project is disposed.
   * @see PsiDocumentManager#hasUncommitedDocuments()
   */
  @Nonnull
  @Contract(pure = true)
  AppUIExecutor withDocumentsCommitted(@Nonnull Project project);

  /**
   * @return an executor that invokes runnables only when indices have been built and are available to use. Automatically expires when the project is disposed.
   * @see com.intellij.openapi.project.DumbService#isDumb(Project)
   */
  @Nonnull
  @Contract(pure = true)
  AppUIExecutor inSmartMode(@Nonnull Project project);

  /**
   * @return an executor that invokes runnables only in transaction. Automatically expires when {@code parentDisposable} is disposed.
   * @see TransactionGuard#submitTransaction(Disposable, Runnable)
   */
  @Nonnull
  @Contract(pure = true)
  AppUIExecutor inTransaction(@Nonnull Disposable parentDisposable);
}
