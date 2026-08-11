package consulo.web.internal;

import consulo.application.impl.internal.UnifiedApplication;
import consulo.application.internal.StartupProgress;
import consulo.component.internal.ComponentBinding;
import consulo.logging.Logger;
import com.vaadin.flow.component.UI;
import consulo.ui.UIAccess;
import consulo.web.internal.ui.base.VaadinComponentDelegate;
import consulo.util.lang.ref.SimpleReference;
import consulo.web.internal.ui.WebUnboundUIAccess;
import consulo.web.application.WebApplication;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;

/**
 * @author VISTALL
 * @since 16-Sep-17
 */
public class WebApplicationImpl extends UnifiedApplication implements WebApplication {
  private static final Logger LOG = Logger.getInstance(WebApplicationImpl.class);

  private final WebUnboundUIAccess myUnboundUIAccess = new WebUnboundUIAccess();

  private final Set<UIAccess> myUIAccesses = new LinkedHashSet<>();

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
    getLastUIAccess().give(runnable);
  }

  @Override
  public void invokeLater(Runnable runnable, BooleanSupplier expired) {
    getLastUIAccess().give(runnable);
  }

  @Override
  public void invokeLater(Runnable runnable, consulo.ui.ModalityState state) {
    getLastUIAccess().give(runnable);
  }

  @Override
  public void invokeLater(Runnable runnable, consulo.ui.ModalityState state, BooleanSupplier expired) {
    getLastUIAccess().give(runnable);
  }

  
  /**
   * The ui a caller already runs in is the one it means. Only a caller which has none - a background thread with
   * no project to name - falls back to the ui which attached last.
   */
  @Override
  public UIAccess getLastUIAccess() {
    // not UIAccess#isUIThread, which also answers for a request which only holds the session lock - the access of
    // a ui can only be taken while that ui is the current one
    UI ui = UI.getCurrent();
    if (ui != null) {
      return VaadinComponentDelegate.getUIAccess(ui);
    }

    Collection<UIAccess> uiAccesses = getUIAccesses();
    return uiAccesses.isEmpty() ? myUnboundUIAccess : List.copyOf(uiAccesses).getLast();
  }

  /**
   * Adds a ui which has just been attached. Kept in the order they arrived, so the newest of them is what a caller
   * without a ui of its own is answered with.
   */
  public void registerUIAccess(UIAccess uiAccess) {
    synchronized (myUIAccesses) {
      myUIAccesses.removeIf(access -> !access.isValid());
      myUIAccesses.add(uiAccess);
    }
  }

  /**
   * Every ui still attached. A tab which went away is dropped while answering, since a browser is not obliged to
   * say goodbye.
   */
  public Collection<UIAccess> getUIAccesses() {
    synchronized (myUIAccesses) {
      myUIAccesses.removeIf(access -> !access.isValid());
      return List.copyOf(myUIAccesses);
    }
  }
}
