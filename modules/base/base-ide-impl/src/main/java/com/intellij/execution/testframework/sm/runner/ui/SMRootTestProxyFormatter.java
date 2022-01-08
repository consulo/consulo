package com.intellij.execution.testframework.sm.runner.ui;

import com.intellij.execution.testframework.sm.runner.SMTestProxy;
import javax.annotation.Nonnull;

public interface SMRootTestProxyFormatter {
  void format(@Nonnull SMTestProxy.SMRootTestProxy testProxy, @Nonnull TestTreeRenderer renderer);
}
