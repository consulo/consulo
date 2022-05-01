package consulo.ide.impl.idea.execution.testframework.sm.runner.ui;

import consulo.ide.impl.idea.execution.testframework.sm.runner.SMTestProxy;
import javax.annotation.Nonnull;

public interface SMRootTestProxyFormatter {
  void format(@Nonnull SMTestProxy.SMRootTestProxy testProxy, @Nonnull TestTreeRenderer renderer);
}
