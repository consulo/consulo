package consulo.web.application.impl;

import com.intellij.ide.StartupProgress;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.TransactionGuard;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Ref;
import consulo.ui.RequiredUIAccess;
import consulo.annotations.RequiredReadAction;
import consulo.application.DummyTransactionGuard;
import consulo.application.impl.BaseApplicationWithOwnWriteThread;
import consulo.injecting.InjectingContainerBuilder;
import consulo.ui.UIAccess;
import consulo.web.application.WebApplication;
import consulo.web.application.WebSession;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;

/**
 * @author VISTALL
 * @since 16-Sep-17
 */
public class WebApplicationImpl extends BaseApplicationWithOwnWriteThread implements WebApplication {
  private WebSession myCurrentSession;
  private static final ModalityState ANY = new ModalityState() {
    @Override
    public boolean dominates(@Nonnull ModalityState anotherState) {
      return false;
    }

    @NonNls
    @Override
    public String toString() {
      return "ANY";
    }
  };

  public WebApplicationImpl(@Nonnull Ref<? extends StartupProgress> splash) {
    super(splash);

    ApplicationManager.setApplication(this);
  }

  @Override
  protected void bootstrapInjectingContainer(@Nonnull InjectingContainerBuilder builder) {
    super.bootstrapInjectingContainer(builder);

    builder.bind(TransactionGuard.class).to(new DummyTransactionGuard());
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

  @Override
  public void exit() {

  }

  @Override
  public void invokeLater(@Nonnull Runnable runnable) {
    getCurrentSession().getAccess().give(runnable);
  }

  @Override
  public void invokeLater(@Nonnull Runnable runnable, @Nonnull Condition expired) {
    getCurrentSession().getAccess().give(runnable);
  }

  @Override
  public void invokeLater(@Nonnull Runnable runnable, @Nonnull ModalityState state) {
    getCurrentSession().getAccess().give(runnable);
  }

  @Override
  public void invokeLater(@Nonnull Runnable runnable, @Nonnull ModalityState state, @Nonnull Condition expired) {
    getCurrentSession().getAccess().give(runnable);
  }

  @Override
  public void invokeAndWait(@Nonnull Runnable runnable, @Nonnull ModalityState modalityState) {
    getCurrentSession().getAccess().giveAndWait(runnable);
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
  public boolean runProcessWithProgressSynchronously(@Nonnull Runnable process, @Nonnull String progressTitle, boolean canBeCanceled, Project project) {
    return false;
  }

  @RequiredUIAccess
  @Override
  public boolean runProcessWithProgressSynchronously(@Nonnull Runnable process, @Nonnull String progressTitle, boolean canBeCanceled, @Nullable Project project, JComponent parentComponent) {
    return false;
  }

  @RequiredUIAccess
  @Override
  public boolean runProcessWithProgressSynchronously(@Nonnull Runnable process,
                                                     @Nonnull String progressTitle,
                                                     boolean canBeCanceled,
                                                     @Nullable Project project,
                                                     JComponent parentComponent,
                                                     String cancelText) {

    WebModalProgressIndicator progress = new WebModalProgressIndicator();

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


    return true;
  }

  @Override
  public void assertTimeConsuming() {

  }

  @Nonnull
  @Override
  public UIAccess getLastUIAccess() {
    WebSession currentSession = getCurrentSession();
    if(currentSession == null) {
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
}
