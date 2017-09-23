package consulo.web.application.impl;

import com.intellij.openapi.application.impl.ApplicationImpl;
import com.intellij.ui.Splash;
import consulo.web.application.WebApplication;
import consulo.web.application.WebSession;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author VISTALL
 * @since 16-Sep-17
 */
public class WebApplicationImpl extends ApplicationImpl implements WebApplication {
  private WebSession myCurrentSession;

  public WebApplicationImpl(boolean isInternal, boolean isUnitTestMode, boolean isHeadless, boolean isCommandLine, @NotNull String appName, @Nullable Splash splash) {
    super(isInternal, isUnitTestMode, isHeadless, isCommandLine, appName, splash);
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
