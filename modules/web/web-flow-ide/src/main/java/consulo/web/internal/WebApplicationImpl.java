package consulo.web.internal;

import consulo.application.impl.internal.UnifiedApplication;
import consulo.application.internal.StartupProgress;
import consulo.component.internal.ComponentBinding;
import consulo.logging.Logger;
import consulo.ui.UIAccess;
import consulo.util.lang.ref.SimpleReference;
import consulo.web.internal.ui.WebUnboundUIAccess;
import consulo.web.application.WebApplication;
import consulo.web.application.WebSession;
import org.jspecify.annotations.Nullable;

import java.util.function.BooleanSupplier;

/**
 * @author VISTALL
 * @since 16-Sep-17
 */
public class WebApplicationImpl extends UnifiedApplication implements WebApplication {
  private static final Logger LOG = Logger.getInstance(WebApplicationImpl.class);

  private final WebUnboundUIAccess myUnboundUIAccess = new WebUnboundUIAccess();

  private WebSession myCurrentSession;

  public WebApplicationImpl(ComponentBinding componentBinding, SimpleReference<? extends StartupProgress> splash) {
    super(componentBinding, splash);
  }

  public @Nullable WebStartupProgressImpl getSplash() {
    return (WebStartupProgressImpl)mySplashRef.get();
  }

  /**
   * Always defers, even when the caller already is the ui thread - running inline would let a service constructor
   * that posts an initializer re-enter itself before the container finished binding it.
   */
  @Override
  public void invokeLater(Runnable runnable) {
    WebSession currentSession = getCurrentSession();
    if (currentSession != null) currentSession.getAccess().give(runnable);
  }

  @Override
  public void invokeLater(Runnable runnable, BooleanSupplier expired) {
    WebSession currentSession = getCurrentSession();
    if (currentSession != null) currentSession.getAccess().give(runnable);
  }

  @Override
  public void invokeLater(Runnable runnable, consulo.ui.ModalityState state) {
    WebSession currentSession = getCurrentSession();
    if (currentSession != null) currentSession.getAccess().give(runnable);
  }

  @Override
  public void invokeLater(Runnable runnable, consulo.ui.ModalityState state, BooleanSupplier expired) {
    WebSession currentSession = getCurrentSession();
    if (currentSession != null) currentSession.getAccess().give(runnable);
  }

  
  @Override
  public UIAccess getLastUIAccess() {
    WebSession currentSession = getCurrentSession();
    if (currentSession == null) {
      return myUnboundUIAccess;
    }
    return currentSession.getAccess();
  }

  @Override
  public void setCurrentSession(@Nullable WebSession session) {
    myCurrentSession = session;
  }

  @Override
  public @Nullable WebSession getCurrentSession() {
    return myCurrentSession;
  }
}
