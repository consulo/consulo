package consulo.execution.test.sm.ui;

import consulo.execution.test.sm.runner.SMTestProxy;

public interface SMRootTestProxyFormatter {
    void format(SMTestProxy.SMRootTestProxy testProxy, TestTreeRenderer renderer);
}
