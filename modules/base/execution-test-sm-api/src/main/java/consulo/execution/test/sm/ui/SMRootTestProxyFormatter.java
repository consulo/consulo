package consulo.execution.test.sm.ui;

import consulo.execution.test.sm.runner.SMTestProxy;

import javax.annotation.Nonnull;

public interface SMRootTestProxyFormatter {
  void format(@Nonnull SMTestProxy.SMRootTestProxy testProxy, @Nonnull TestTreeRenderer renderer);
}
