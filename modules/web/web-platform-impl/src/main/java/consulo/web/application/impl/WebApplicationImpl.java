package consulo.web.application.impl;

import com.intellij.openapi.application.impl.ApplicationImpl;
import com.intellij.ui.Splash;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author VISTALL
 * @since 16-Sep-17
 */
public class WebApplicationImpl extends ApplicationImpl {
  public WebApplicationImpl(boolean isInternal, boolean isUnitTestMode, boolean isHeadless, boolean isCommandLine, @NotNull String appName, @Nullable Splash splash) {
    super(isInternal, isUnitTestMode, isHeadless, isCommandLine, appName, splash);
  }
}
