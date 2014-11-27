package org.mustbe.consulo.execution.testframework.thrift.runner;

import com.intellij.execution.testframework.sm.runner.GeneralTestEventsProcessor;

/**
 * @author VISTALL
 * @since 18.05.14
 */
public class ThriftTestHandlerFactory {
  private final int myPort;

  public ThriftTestHandlerFactory() {
    this(ThriftTestExecutionUtil.getFreePort());
  }

  public ThriftTestHandlerFactory(int port) {
    myPort = port;
  }

  public BaseThriftTestHandler createHandler(GeneralTestEventsProcessor processor) {
    return new BaseThriftTestHandler(processor);
  }

  public int getPort() {
    return myPort;
  }
}
