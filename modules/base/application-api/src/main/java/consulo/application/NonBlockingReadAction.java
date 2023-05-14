// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.application;

import consulo.application.progress.ProgressIndicator;
import consulo.component.ComponentManager;
import consulo.component.ProcessCanceledException;
import consulo.disposer.Disposable;
import consulo.ui.ModalityState;
import consulo.util.concurrent.CancellablePromise;
import consulo.util.dataholder.Key;
import org.jetbrains.annotations.Contract;

import jakarta.annotation.Nonnull;
import java.util.concurrent.Executor;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A utility for running non-blocking read actions in background thread.
 * "Non-blocking" means to prevent UI freezes, when a write action is about to occur, a read action can be interrupted by a
 * {@link ProcessCanceledException} and then restarted.
 *
 * @see ReadAction#nonBlocking
 */
public interface NonBlockingReadAction<T> {

  /**
   * @return a copy of this builder that runs read actions only when index is available in the given project.
   * The operation is canceled if the project is closed before either the background computation or {@link #finishOnUiThread} runnable
   * are completed.
   * @see consulo.ide.impl.idea.openapi.project.DumbService
   */
  @Contract(pure = true)
  NonBlockingReadAction<T> inSmartMode(@Nonnull ComponentManager project);

  /**
   * @return a copy of this builder that runs read actions only when all documents are committed.
   * The operation is canceled if the project is closed before either the background computation or {@link #finishOnUiThread} runnable
   * are completed.
   * @see com.intellij.psi.PsiDocumentManager
   */
  @Contract(pure = true)
  NonBlockingReadAction<T> withDocumentsCommitted(@Nonnull ComponentManager project);

  /**
   * @return a copy of this builder that cancels submitted read actions after they become obsolete.
   * An action is considered obsolete if any of the conditions provided using {@code expireWhen} returns true).
   * The conditions are checked inside a read action, either on a background or on the UI thread.
   */
  @Contract(pure = true)
  NonBlockingReadAction<T> expireWhen(@Nonnull BooleanSupplier expireCondition);

  /**
   * @return a copy of this builder that cancels submitted read actions once the specified disposable is disposed.
   */
  @Contract(pure = true)
  NonBlockingReadAction<T> expireWith(@Nonnull Disposable parentDisposable);

  /**
   * @return a copy of this builder that synchronizes the specified progress indicator with the inner one created by {@link NonBlockingReadAction}.
   * This means that submitted read actions are cancelled once the outer indicator is cancelled,
   * and the visual changes (e.g. {@link ProgressIndicator#setText}) are propagated from the inner to the outer indicator.
   */
  @Contract(pure = true)
  @Nonnull
  NonBlockingReadAction<T> wrapProgress(@Nonnull ProgressIndicator progressIndicator);

  /**
   * @return a copy of this builder that completes submitted read actions on UI thread with the given modality state.
   * The read actions are still executed on background thread, but the callbacks on their completion
   * are invoked on UI thread, and no write action is allowed to interfere before that and possibly invalidate the result.
   */
  @Contract(pure = true)
  NonBlockingReadAction<T> finishOnUiThread(@Nonnull Function<Application, ModalityState> modalityGetter, @Nonnull Consumer<? super T> uiThreadAction);

  /**
   * Merges together similar computations by cancelling the previous ones when a new one is submitted.
   * This can be useful when the results of the previous computation won't make sense anyway in the changed environment.
   *
   * @param equality objects that together identify the computation: if they're all equal in two submissions,
   *                 then the computations are merged. Callers should take care to pass something unique there
   *                 (e.g. some {@link Key} or {@code this} {@code getClass()}),
   *                 so that computations from different places won't interfere.
   * @return a copy of this builder which, when submitted, cancels previously submitted running computations with equal equality objects
   */
  @Contract(pure = true)
  NonBlockingReadAction<T> coalesceBy(@Nonnull Object... equality);

  /**
   * Submit this computation to be performed in a non-blocking read action on background thread. The returned promise
   * is completed on the same thread (in the same read action), or on UI thread if {@link #finishOnUiThread} has been called.
   *
   * @param backgroundThreadExecutor an executor to actually run the computation. Common examples are
   *                                 {@link consulo.ide.impl.idea.util.concurrency.NonUrgentExecutor#getInstance()} or
   *                                 {@link AppExecutorUtil#getAppExecutorService()} or
   *                                 {@link consulo.ide.impl.idea.util.concurrency.BoundedTaskExecutor} on top of that.
   */
  CancellablePromise<T> submit(@Nonnull Executor backgroundThreadExecutor);

  /**
   * Run this computation on the current thread in a non-blocking read action, when possible.
   * Note: this method can throw various exceptions (see "Throws" section)
   * and can block the current thread for an indefinite amount of time with just waiting,
   * which can lead to thread starvation or unnecessary thread pool expansion.
   * Besides that, after a read action is finished, a write action in another thread can occur at any time and make the
   * just computed value obsolete.
   * Therefore, it's advised to use asynchronous {@link #submit} API where possible,
   * preferably coupled with {@link #finishOnUiThread} to ensure result validity.<p></p>
   * <p>
   * If the current thread already has read access, the computation is executed as is, without any write-action-cancellability.
   * It's the responsibility of the caller to take care about it.<p></p>
   * <p>
   * {@link #finishOnUiThread} and {@link #coalesceBy} are not supported with synchronous non-blocking read actions.
   *
   * @return the result of the computation
   * @throws ProcessCanceledException if the computation got expired due to {@link #expireWhen} or {@link #expireWith} or {@link #wrapProgress}.
   * @throws IllegalStateException    if current thread already has read access and the constraints (e.g. {@link #inSmartMode} are not satisfied)
   * @throws RuntimeException         when the computation throws an exception. If it's a checked one, it's wrapped into a {@link RuntimeException}.
   */
  T executeSynchronously() throws ProcessCanceledException;
}
