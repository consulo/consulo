/*
 * Copyright 2013-2016 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.execution.testframework.thrift.runner;

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
