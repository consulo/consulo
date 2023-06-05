package consulo.web.internal;

import consulo.application.impl.internal.IdeaModalityState;
import consulo.application.impl.internal.UnifiedApplication;
import consulo.application.impl.internal.start.StartupProgress;
import consulo.logging.Logger;
import consulo.ui.UIAccess;
import consulo.util.lang.ref.SimpleReference;
import consulo.web.application.WebApplication;
import consulo.web.application.WebSession;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.awt.*;
import java.util.function.BooleanSupplier;

/**
 * @author VISTALL
 * @since 16-Sep-17
 */
public class WebApplicationImpl extends UnifiedApplication implements WebApplication {
  private static final Logger LOG = Logger.getInstance(WebApplicationImpl.class);

  private WebSession myCurrentSession;
  private static final IdeaModalityState ANY = new IdeaModalityState() {
    @Override
    public boolean dominates(@Nonnull IdeaModalityState anotherState) {
      return false;
    }

    @Override
    public String toString() {
      return "ANY";
    }
  };

  public WebApplicationImpl(@Nonnull SimpleReference<? extends StartupProgress> splash) {
    super(splash);
  }

  @Nullable
  public WebStartupProgressImpl getSplash() {
    return (WebStartupProgressImpl)mySplashRef.get();
  }

  @Override
  @Nonnull
  public IdeaModalityState getAnyModalityState() {
    return ANY;
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
  public IdeaModalityState getCurrentModalityState() {
    return getNoneModalityState();
  }

  @Nonnull
  @Override
  public IdeaModalityState getModalityStateForComponent(@Nonnull Component c) {
    return getNoneModalityState();
  }

  @Nonnull
  @Override
  public IdeaModalityState getDefaultModalityState() {
    return getNoneModalityState();
  }

  @Nonnull
  @Override
  public IdeaModalityState getNoneModalityState() {
    return IdeaModalityState.NON_MODAL;
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
