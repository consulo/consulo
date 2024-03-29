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
package consulo.desktop.awt.application.impl;

import com.google.common.base.MoreObjects;
import consulo.application.internal.ApplicationEx;
import consulo.application.impl.internal.IdeaModalityState;
import consulo.component.ProcessCanceledException;
import consulo.application.util.registry.Registry;
import consulo.application.util.Semaphore;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.application.AccessToken;
import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.application.TransactionId;
import consulo.application.internal.TransactionGuardEx;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.logging.Logger;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author peter
 */
public class DesktopTransactionGuardImpl extends TransactionGuardEx {
  private static final Logger LOG = Logger.getInstance(DesktopTransactionGuardImpl.class);
  private final Queue<Transaction> myQueue = new LinkedBlockingQueue<>();
  private final Map<consulo.ui.ModalityState, TransactionIdImpl> myModality2Transaction = ContainerUtil.createConcurrentWeakMap();

  /**
   * Remembers the value of {@link #myWritingAllowed} at the start of each modality. If writing wasn't allowed at that moment
   * (e.g. inside SwingUtilities.invokeLater), it won't be allowed for all dialogs inside such modality, even from user activity.
   */
  private final Map<consulo.ui.ModalityState, Boolean> myWriteSafeModalities = ContainerUtil.createConcurrentWeakMap();
  private TransactionIdImpl myCurrentTransaction;
  private boolean myWritingAllowed;
  private boolean myErrorReported;
  private static boolean ourTestingTransactions;

  public DesktopTransactionGuardImpl() {
    myWriteSafeModalities.put(IdeaModalityState.NON_MODAL, true);
  }

  @Nonnull
  private Queue<Transaction> getQueue(@Nullable TransactionIdImpl transaction) {
    while (transaction != null && transaction.myFinished) {
      transaction = transaction.myParent;
    }
    return transaction == null ? myQueue : transaction.myQueue;
  }

  private void pollQueueLater() {
    invokeLater(() -> {
      Queue<Transaction> queue = getQueue(myCurrentTransaction);
      Transaction next = queue.peek();
      if (next != null && canRunTransactionNow(next, false)) {
        queue.remove();
        runSyncTransaction(next);
      }
    });
  }

  private void runSyncTransaction(@Nonnull Transaction transaction) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    if (Disposer.isDisposed(transaction.parentDisposable)) return;

    boolean wasWritingAllowed = myWritingAllowed;
    myWritingAllowed = true;
    myCurrentTransaction = new TransactionIdImpl(myCurrentTransaction);

    try {
      transaction.runnable.run();
    }
    finally {
      Queue<Transaction> queue = getQueue(myCurrentTransaction.myParent);
      queue.addAll(myCurrentTransaction.myQueue);
      if (!queue.isEmpty()) {
        pollQueueLater();
      }

      myWritingAllowed = wasWritingAllowed;
      myCurrentTransaction.myFinished = true;
      myCurrentTransaction = myCurrentTransaction.myParent;
    }
  }

  @Override
  public void submitTransaction(@Nonnull Disposable parentDisposable, @Nullable TransactionId expectedContext, @Nonnull Runnable _transaction) {
    final TransactionIdImpl expectedId = (TransactionIdImpl)expectedContext;
    final Transaction transaction = new Transaction(_transaction, expectedId, parentDisposable);
    final Application app = ApplicationManager.getApplication();
    final boolean isDispatchThread = app.isDispatchThread();
    Runnable runnable = () -> {
      if (canRunTransactionNow(transaction, isDispatchThread)) {
        runSyncTransaction(transaction);
      }
      else {
        getQueue(expectedId).offer(transaction);
        pollQueueLater();
      }
    };

    if (isDispatchThread) {
      runnable.run();
    } else {
      invokeLater(runnable);
    }
  }

  private boolean canRunTransactionNow(Transaction transaction, boolean sync) {
    if (sync && !myWritingAllowed) {
      return false;
    }

    TransactionIdImpl currentId = myCurrentTransaction;
    if (currentId == null) {
      return true;
    }

    return transaction.expectedContext != null && currentId.myStartCounter <= transaction.expectedContext.myStartCounter;
  }

  @Override
  public void submitTransactionAndWait(@Nonnull final Runnable runnable) throws ProcessCanceledException {
    Application app = ApplicationManager.getApplication();
    if (app.isDispatchThread()) {
      Transaction transaction = new Transaction(runnable, getContextTransaction(), app);
      if (!canRunTransactionNow(transaction, true)) {
        String message = "Cannot run synchronous submitTransactionAndWait from invokeLater. " +
                         "Please use asynchronous submit*Transaction. " +
                         "See TransactionGuard FAQ for details.\nTransaction: " + runnable;
        if (!isWriteSafeModality(IdeaModalityState.current())) {
          message += "\nUnsafe modality: " + IdeaModalityState.current();
        }
        LOG.error(message);
      }
      runSyncTransaction(transaction);
      return;
    }

    if (app.isReadAccessAllowed()) {
      throw new IllegalStateException("submitTransactionAndWait should not be invoked from a read action");
    }
    final Semaphore semaphore = new Semaphore();
    semaphore.down();
    final Throwable[] exception = {null};
    submitTransaction(Disposable.newDisposable("never disposed"), getContextTransaction(), () -> {
      try {
        runnable.run();
      }
      catch (Throwable e) {
        exception[0] = e;
      }
      finally {
        semaphore.up();
      }
    });
    semaphore.waitFor();
    if (exception[0] != null) {
      throw new RuntimeException(exception[0]);
    }
  }

  /**
   * An absolutely guru method!<p/>
   *
   * Executes the given code and marks it as a user activity, to allow write actions to be run without requiring transactions.
   * This is only to be called from UI infrastructure, during InputEvent processing and wrap the point where the control
   * goes to custom input event handlers for the first time.<p/>
   *
   * If you wish to invoke some actionPerformed,
   * please consider using {@code ActionManager.tryToExecute()} instead, or ensure in some other way that the action is enabled
   * and can be invoked in the current modality state.
   */
  @Override
  public void performUserActivity(Runnable activity) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    AccessToken token = startActivity(true);
    try {
      activity.run();
    }
    finally {
      token.finish();
    }
  }

  /**
   * An absolutely guru method, only intended to be used from Swing event processing. Please consult Peter if you think you need to invoke this.
   */
  @Override
  @Nonnull
  public AccessToken startActivity(boolean userActivity) {
    myErrorReported = false;
    boolean allowWriting = userActivity && isWriteSafeModality(IdeaModalityState.current());
    if (myWritingAllowed == allowWriting) {
      return AccessToken.EMPTY_ACCESS_TOKEN;
    }

    ApplicationManager.getApplication().assertIsDispatchThread();
    final boolean prev = myWritingAllowed;
    myWritingAllowed = allowWriting;
    return new AccessToken() {
      @Override
      public void finish() {
        myWritingAllowed = prev;
      }
    };
  }

  @Override
  public boolean isWriteSafeModality(consulo.ui.ModalityState state) {
    return Boolean.TRUE.equals(myWriteSafeModalities.get(state));
  }

  @Override
  public void assertWriteActionAllowed() {
    ApplicationManager.getApplication().assertIsWriteThread();
    if (areAssertionsEnabled() && !myWritingAllowed && !myErrorReported) {
      // please assign exceptions here to Peter
      LOG.error(reportWriteUnsafeContext(IdeaModalityState.current()));
      myErrorReported = true;
    }
  }

  private String reportWriteUnsafeContext(@Nonnull consulo.ui.ModalityState modality) {
    return "Write-unsafe context! Model changes are allowed from write-safe contexts only. " +
           "Please ensure you're using invokeLater/invokeAndWait with a correct modality state (not \"any\"). " +
           "See TransactionGuard documentation for details." +
           "\n  current modality=" + modality +
           "\n  known modalities=" + myWriteSafeModalities;
  }

  @Override
  public void assertWriteSafeContext(@Nonnull consulo.ui.ModalityState modality) {
    if (!isWriteSafeModality(modality) && areAssertionsEnabled()) {
      // please assign exceptions here to Peter
      LOG.error(reportWriteUnsafeContext(modality));
    }
  }

  private static boolean areAssertionsEnabled() {
    Application app = ApplicationManager.getApplication();
    if (app.isUnitTestMode() && !ourTestingTransactions) {
      return false;
    }
    if (app instanceof ApplicationEx && !((ApplicationEx)app).isLoaded()) {
      return false;
    }
    return Registry.is("ide.require.transaction.for.model.changes", false);
  }

  @Override
  public void submitTransactionLater(@Nonnull final Disposable parentDisposable, @Nonnull final Runnable transaction) {
    final TransactionIdImpl id = getContextTransaction();
    final IdeaModalityState startModality = IdeaModalityState.defaultModalityState();
    invokeLater(() -> {
      boolean allowWriting = IdeaModalityState.current() == startModality;
      AccessToken token = startActivity(allowWriting);
      try {
        submitTransaction(parentDisposable, id, transaction);
      }
      finally {
        token.finish();
      }
    });
  }

  private static void invokeLater(Runnable runnable) {
    ApplicationManager.getApplication().invokeLater(runnable, IdeaModalityState.any(), () -> false);
  }

  @Override
  public TransactionIdImpl getContextTransaction() {
    if (!ApplicationManager.getApplication().isDispatchThread()) {
      return myModality2Transaction.get(IdeaModalityState.defaultModalityState());
    }

    return myWritingAllowed ? myCurrentTransaction : null;
  }

  @Override
  public void enteredModality(@Nonnull consulo.ui.ModalityState modality) {
    TransactionIdImpl contextTransaction = getContextTransaction();
    if (contextTransaction != null) {
      myModality2Transaction.put(modality, contextTransaction);
    }
    myWriteSafeModalities.put(modality, myWritingAllowed);
  }

  @Override
  @Nullable
  public TransactionIdImpl getModalityTransaction(@Nonnull consulo.ui.ModalityState modalityState) {
    return myModality2Transaction.get(modalityState);
  }

  @Nonnull
  public Runnable wrapLaterInvocation(@Nonnull final Runnable runnable, @Nonnull IdeaModalityState modalityState) {
    if (isWriteSafeModality(modalityState)) {
      return new Runnable() {
        @Override
        public void run() {
          ApplicationManager.getApplication().assertIsWriteThread();
          final boolean prev = myWritingAllowed;
          myWritingAllowed = true;
          try {
            runnable.run();
          } finally {
            myWritingAllowed = prev;
          }
        }

        @Override
        public String toString() {
          return runnable.toString();
        }
      };
    }

    return runnable;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
            .add("currentTransaction", myCurrentTransaction)
            .add("writingAllowed", myWritingAllowed)
            .toString();
  }

  public static void setTestingTransactions(boolean testingTransactions) {
    ourTestingTransactions = testingTransactions;
  }

  private static class Transaction {
    @Nonnull
    final Runnable runnable;
    @Nullable final TransactionIdImpl expectedContext;
    @Nonnull
    final Disposable parentDisposable;

    Transaction(@Nonnull Runnable runnable, @Nullable TransactionIdImpl expectedContext, @Nonnull Disposable parentDisposable) {
      this.runnable = runnable;
      this.expectedContext = expectedContext;
      this.parentDisposable = parentDisposable;
    }
  }

  private static class TransactionIdImpl implements TransactionId {
    private static final AtomicLong ourTransactionCounter = new AtomicLong();
    final long myStartCounter = ourTransactionCounter.getAndIncrement();
    final Queue<Transaction> myQueue = new LinkedBlockingQueue<>();
    boolean myFinished;
    final TransactionIdImpl myParent;

    public TransactionIdImpl(@Nullable TransactionIdImpl parent) {
      myParent = parent;
    }

    @Override
    public String toString() {
      return "Transaction " + myStartCounter + (myFinished ? "(finished)" : "");
    }
  }
}