package consulo.web.application.impl;

import com.intellij.ide.StartupProgress;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.TransactionGuard;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.util.ProgressWindow;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import consulo.annotation.access.RequiredReadAction;
import consulo.application.impl.BaseApplication;
import consulo.disposer.Disposer;
import consulo.injecting.InjectingContainerBuilder;
import consulo.logging.Logger;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.lang.ref.SimpleReference;
import consulo.web.application.WebApplication;
import consulo.web.application.WebSession;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author VISTALL
 * @since 16-Sep-17
 */
public class WebApplicationImpl extends BaseApplication implements WebApplication {
  private static final Logger LOG = Logger.getInstance(WebApplicationImpl.class);

  private WebSession myCurrentSession;
  private static final ModalityState ANY = new ModalityState() {
    @Override
    public boolean dominates(@Nonnull ModalityState anotherState) {
      return false;
    }

    @Override
    public String toString() {
      return "ANY";
    }
  };

  public WebApplicationImpl(@Nonnull SimpleReference<? extends StartupProgress> splash) {
    super(splash);

    ApplicationManager.setApplication(this);
  }

  @Override
  protected void bootstrapInjectingContainer(@Nonnull InjectingContainerBuilder builder) {
    super.bootstrapInjectingContainer(builder);

    builder.bind(TransactionGuard.class).to(new WebTransactionGuardImpl());
  }

  @Nullable
  public WebStartupProgressImpl getSplash() {
    return (WebStartupProgressImpl)mySplashRef.get();
  }

  @Override
  @Nonnull
  public ModalityState getAnyModalityState() {
    return ANY;
  }

  @RequiredReadAction
  @Override
  public void assertReadAccessAllowed() {
    if (!isReadAccessAllowed()) {
      throw new IllegalArgumentException();
    }
  }

  @RequiredUIAccess
  @Override
  public void assertIsDispatchThread() {
    if (!isDispatchThread()) {
      throw new IllegalArgumentException(Thread.currentThread().getName() + " is not ui thread");
    }
  }

  @RequiredUIAccess
  @Override
  public void assertIsWriteThread() {
    if (!isWriteThread()) {
      throw new IllegalArgumentException(Thread.currentThread().getName() + " is not write thread");
    }
  }

  @Override
  public void exit() {

  }

  @Override
  public void invokeLater(@Nonnull Runnable runnable) {
    WebSession currentSession = getCurrentSession();
    if (currentSession != null) currentSession.getAccess().giveIfNeed(runnable);
  }

  @Override
  public void invokeLater(@Nonnull Runnable runnable, @Nonnull Condition expired) {
    WebSession currentSession = getCurrentSession();
    if (currentSession != null) currentSession.getAccess().giveIfNeed(runnable);
  }

  @Override
  public void invokeLater(@Nonnull Runnable runnable, @Nonnull ModalityState state) {
    WebSession currentSession = getCurrentSession();
    if (currentSession != null) currentSession.getAccess().giveIfNeed(runnable);
  }

  @Override
  public void invokeLater(@Nonnull Runnable runnable, @Nonnull ModalityState state, @Nonnull Condition expired) {
    WebSession currentSession = getCurrentSession();
    if (currentSession != null) currentSession.getAccess().giveIfNeed(runnable);
  }

  @Override
  public void invokeAndWait(@Nonnull Runnable runnable, @Nonnull ModalityState modalityState) {
    WebSession currentSession = getCurrentSession();
    if (currentSession != null) currentSession.getAccess().giveAndWaitIfNeed(runnable);
  }

  @Nonnull
  @Override
  public ModalityState getCurrentModalityState() {
    return getNoneModalityState();
  }

  @Nonnull
  @Override
  public ModalityState getModalityStateForComponent(@Nonnull Component c) {
    return getNoneModalityState();
  }

  @Nonnull
  @Override
  public ModalityState getDefaultModalityState() {
    return getNoneModalityState();
  }

  @Nonnull
  @Override
  public ModalityState getNoneModalityState() {
    return ModalityState.NON_MODAL;
  }

  @RequiredUIAccess
  @Override
  public long getIdleTime() {
    return 0;
  }

  @Override
  public boolean isHeadlessEnvironment() {
    return false;
  }

  @Override
  public boolean isDisposeInProgress() {
    return false;
  }

  @Override
  public boolean isRestartCapable() {
    return true;
  }

  @Override
  public void restart() {

  }

  @Override
  public boolean isActive() {
    return true;
  }

  @Override
  public void exit(boolean force, boolean exitConfirmed) {

  }

  @Override
  public void restart(boolean exitConfirmed) {

  }

  @RequiredUIAccess
  @Override
  public boolean runProcessWithProgressSynchronously(@Nonnull Runnable process,
                                                     @Nonnull String progressTitle,
                                                     boolean canBeCanceled,
                                                     @Nullable Project project,
                                                     JComponent parentComponent,
                                                     String cancelText) {

    assertIsDispatchThread();
    boolean writeAccessAllowed = isWriteAccessAllowed();
    if (writeAccessAllowed // Disallow running process in separate thread from under write action.
        // The thread will deadlock trying to get read action otherwise.
        || isHeadlessEnvironment() && !isUnitTestMode()) {
      if (writeAccessAllowed) {
        LOG.debug("Starting process with progress from within write action makes no sense");
      }
      try {
        ProgressManager.getInstance().runProcess(process, new EmptyProgressIndicator());
      }
      catch (ProcessCanceledException e) {
        // ok to ignore.
        return false;
      }
      return true;
    }

    final ProgressWindow progress = new ProgressWindow(canBeCanceled, false, project, parentComponent, cancelText);
    // in case of abrupt application exit when 'ProgressManager.getInstance().runProcess(process, progress)' below
    // does not have a chance to run, and as a result the progress won't be disposed
    Disposer.register(this, progress);

    progress.setTitle(progressTitle);

    final AtomicBoolean threadStarted = new AtomicBoolean();
    //noinspection SSBasedInspection
    getLastUIAccess().give(() -> {
      executeOnPooledThread(() -> {
        try {
          ProgressManager.getInstance().runProcess(process, progress);
        }
        catch (ProcessCanceledException e) {
          progress.cancel();
          // ok to ignore.
        }
        catch (RuntimeException e) {
          progress.cancel();
          throw e;
        }
      });
      threadStarted.set(true);
    });

    progress.startBlocking();

    LOG.assertTrue(threadStarted.get());
    LOG.assertTrue(!progress.isRunning());

    return !progress.isCanceled();
  }

  @RequiredUIAccess
  @Override
  public void assertIsDispatchThread(@Nullable JComponent component) {

  }

  @Override
  public void assertTimeConsuming() {

  }

  @Nonnull
  @Override
  public UIAccess getLastUIAccess() {
    WebSession currentSession = getCurrentSession();
    if (currentSession == null) {
      throw new IllegalArgumentException("No session");
    }
    return currentSession.getAccess();
  }

  @Override
  public void setCurrentSession(@Nullable WebSession session) {
    myCurrentSession = session;
  }

  @Override
  @Nullable
  public WebSession getCurrentSession() {
    return myCurrentSession;
  }

  @Override
  public void executeSuspendingWriteAction(@Nullable Project project, @Nonnull String title, @Nonnull Runnable runnable) {

  }

  @Override
  public boolean isReadAccessAllowed() {
    if (isDispatchThread()) {
      return true;
    }
    return isWriteThread() || myLock.isReadLockedByThisThread();
  }

  @Override
  public boolean isDispatchThread() {
    return UIAccess.isUIThread();
  }
}
