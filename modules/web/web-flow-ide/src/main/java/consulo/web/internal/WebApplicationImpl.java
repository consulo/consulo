package consulo.web.internal;

import consulo.application.impl.internal.UnifiedApplication;
import consulo.application.internal.StartupProgress;
import consulo.component.impl.internal.ComponentBinding;
import consulo.logging.Logger;
import consulo.ui.UIAccess;
import consulo.util.lang.ref.SimpleReference;
import consulo.web.application.WebApplication;
import consulo.web.application.WebSession;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.function.BooleanSupplier;

/**
 * @author VISTALL
 * @since 16-Sep-17
 */
public class WebApplicationImpl extends UnifiedApplication implements WebApplication {
  private static final Logger LOG = Logger.getInstance(WebApplicationImpl.class);

  private WebSession myCurrentSession;

  public WebApplicationImpl(@Nonnull ComponentBinding componentBinding, @Nonnull SimpleReference<? extends StartupProgress> splash) {
    super(componentBinding, splash);
  }

  @Nullable
  public WebStartupProgressImpl getSplash() {
    return (WebStartupProgressImpl)mySplashRef.get();
  }

  @Override
  public void invokeLater(@Nonnull Runnable runnable) {
    WebSession currentSession = getCurrentSession();
    if (currentSession != null) currentSession.getAccess().giveIfNeed(runnable);
  }

  @Override
  public void invokeLater(@Nonnull Runnable runnable, @Nonnull BooleanSupplier expired) {
    WebSession currentSession = getCurrentSession();
    if (currentSession != null) currentSession.getAccess().giveIfNeed(runnable);
  }

  @Override
  public void invokeLater(@Nonnull Runnable runnable, @Nonnull consulo.ui.ModalityState state) {
    WebSession currentSession = getCurrentSession();
    if (currentSession != null) currentSession.getAccess().giveIfNeed(runnable);
  }

  @Override
  public void invokeLater(@Nonnull Runnable runnable, @Nonnull consulo.ui.ModalityState state, @Nonnull BooleanSupplier expired) {
    WebSession currentSession = getCurrentSession();
    if (currentSession != null) currentSession.getAccess().giveIfNeed(runnable);
  }

  @Override
  public void invokeAndWait(@Nonnull Runnable runnable, @Nonnull consulo.ui.ModalityState modalityState) {
    WebSession currentSession = getCurrentSession();
    if (currentSession != null) currentSession.getAccess().giveAndWaitIfNeed(runnable);
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
}
