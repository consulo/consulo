package consulo.web.application.impl;

import com.intellij.ide.StartupProgress;
import com.intellij.openapi.application.impl.ApplicationImpl;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import consulo.annotations.RequiredDispatchThread;
import consulo.ui.UIAccess;
import consulo.web.application.WebApplication;
import consulo.web.application.WebSession;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author VISTALL
 * @since 16-Sep-17
 */
public class WebApplicationImpl extends ApplicationImpl implements WebApplication {
  private WebSession myCurrentSession;

  public WebApplicationImpl(boolean isInternal, boolean isUnitTestMode, boolean isHeadless, boolean isCommandLine, @NotNull Ref<? extends StartupProgress> splash) {
    super(isInternal, isUnitTestMode, isHeadless, isCommandLine, splash);
  }

  @Nullable
  public WebStartupProgressImpl getSplash() {
    return (WebStartupProgressImpl)mySplashRef.get();
  }

  @Override
  public boolean isDispatchThread() {
    return super.isDispatchThread() || UIAccess.isUIThread();
  }

  @RequiredDispatchThread
  @Override
  public boolean runProcessWithProgressSynchronously(@NotNull Runnable process,
                                                     @NotNull String progressTitle,
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
  public void setCurrentSession(@Nullable WebSession session) {
    myCurrentSession = session;
  }

  @Override
  @Nullable
  public WebSession getCurrentSession() {
    return myCurrentSession;
  }
}
