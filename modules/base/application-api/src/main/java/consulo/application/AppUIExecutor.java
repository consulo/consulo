// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.application;

import consulo.application.internal.AsyncExecutionService;
import consulo.component.ComponentManager;
import consulo.ui.ModalityState;
import org.jetbrains.annotations.Contract;

/**
 * An executor that invokes given runnables on Swing Event Dispatch thread when all constraints of a given set are satisfied at the same time.
 * The executor is created by calling {@link #onUiThread}, the constraints are specified by chained calls. For example, to invoke
 * some action when all documents are committed and indices are available, one can use
 * {@code AppUIExecutor.onUiThread().withDocumentsCommitted(project).inSmartMode(project)}.
 */
public interface AppUIExecutor extends BaseExpirableExecutor<AppUIExecutor> {

  /**
   * Creates an executor working on UI thread.
   */
  static AppUIExecutor onUiThread() {
    return AsyncExecutionService.getService().createUIExecutor();
  }

  /**
   * @deprecated Modality state is no longer used. Use {@link #onUiThread()} instead.
   */
  @Deprecated
  static AppUIExecutor onUiThread(ModalityState modality) {
    return onUiThread();
  }

  /**
   * @return an executor that should always invoke the given runnable later. Otherwise, if {@link #execute} is called
   * on dispatch thread already, and all others constraints are met, the runnable would be executed immediately.
   */
  @Contract(pure = true)
  AppUIExecutor later();

  /**
   * @return an executor that invokes runnables only when all documents are committed. Automatically expires when the project is disposed.
   * @see PsiDocumentManager#hasUncommitedDocuments()
   */
  @Contract(pure = true)
  AppUIExecutor withDocumentsCommitted(ComponentManager project);

  /**
   * @return an executor that invokes runnables only when indices have been built and are available to use. Automatically expires when the project is disposed.
   * @see consulo.ide.impl.idea.openapi.project.DumbService#isDumb(Project)
   */
  @Contract(pure = true)
  AppUIExecutor inSmartMode(ComponentManager project);
}
