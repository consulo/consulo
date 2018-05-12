package consulo.web.application.impl;

import com.intellij.ide.StartupProgress;
import com.intellij.openapi.application.ModalityInvokator;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.TransactionGuard;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Ref;
import consulo.annotations.RequiredDispatchThread;
import consulo.annotations.RequiredReadAction;
import consulo.application.impl.BaseApplicationWithOwnWriteThread;
import consulo.web.application.WebApplication;
import consulo.web.application.WebSession;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 16-Sep-17
 */
public class WebApplicationImpl extends BaseApplicationWithOwnWriteThread implements WebApplication {
  private WebSession myCurrentSession;

  public WebApplicationImpl(boolean isHeadless, @Nonnull Ref<? extends StartupProgress> splash) {
    super(null, splash);

    getPicoContainer().registerComponentInstance(TransactionGuard.class.getName(), new WebTransactionGuardImpl());

    loadApplicationComponents();
  }

  @Nullable
  public WebStartupProgressImpl getSplash() {
    return (WebStartupProgressImpl)mySplashRef.get();
  }

  @RequiredReadAction
  @Override
  public void assertReadAccessAllowed() {
    if (isReadAccessAllowed()) {
      throw new IllegalArgumentException();
    }
  }

  @RequiredDispatchThread
  @Override
  public void assertIsDispatchThread() {
    if (!isDispatchThread()) {
      throw new IllegalArgumentException();
    }
  }

  @Override
  public void exit() {

  }

  @Override
  public void runInWriteThreadAndWait(@Nonnull Runnable runnable) {

  }

  @Nonnull
  @Override
  public ModalityInvokator getInvokator() {
    return null;
  }

  @Override
  public void invokeLater(@Nonnull Runnable runnable) {

  }

  @Override
  public void invokeLater(@Nonnull Runnable runnable, @Nonnull Condition expired) {

  }

  @Override
  public void invokeLater(@Nonnull Runnable runnable, @Nonnull ModalityState state) {

  }

  @Override
  public void invokeLater(@Nonnull Runnable runnable, @Nonnull ModalityState state, @Nonnull Condition expired) {

  }

  @Override
  public void invokeAndWait(@Nonnull Runnable runnable, @Nonnull ModalityState modalityState) {

  }

  @Nonnull
  @Override
  public ModalityState getCurrentModalityState() {
    return null;
  }

  @Nonnull
  @Override
  public ModalityState getModalityStateForComponent(@Nonnull Component c) {
    return null;
  }

  @Nonnull
  @Override
  public ModalityState getDefaultModalityState() {
    return null;
  }

  @Nonnull
  @Override
  public ModalityState getNoneModalityState() {
    return null;
  }

  @Nonnull
  @Override
  public ModalityState getAnyModalityState() {
    return null;
  }

  @RequiredDispatchThread
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

  @RequiredDispatchThread
  @Override
  public boolean runProcessWithProgressSynchronously(@Nonnull Runnable process, @Nonnull String progressTitle, boolean canBeCanceled, Project project) {
    return false;
  }

  @RequiredDispatchThread
  @Override
  public boolean runProcessWithProgressSynchronously(@Nonnull Runnable process, @Nonnull String progressTitle, boolean canBeCanceled, @Nullable Project project, JComponent parentComponent) {
    return false;
  }

  @RequiredDispatchThread
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

  @RequiredDispatchThread
  @Override
  public void assertIsDispatchThread(@Nullable JComponent component) {

  }

  @Override
  public void assertTimeConsuming() {

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
  public boolean runWriteActionWithProgressInDispatchThread(@Nonnull String title,
                                                            @Nullable Project project,
                                                            @Nullable JComponent parentComponent,
                                                            @Nullable String cancelText,
                                                            @Nonnull Consumer<ProgressIndicator> action) {
    return true;
  }
}
