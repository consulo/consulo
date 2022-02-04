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
import com.intellij.execution.testframework.sm.runner.events.*;
import org.apache.thrift.TException;

/**
 * @author VISTALL
 * @since 18.05.14
 */
public class BaseThriftTestHandler implements TestInterface.Iface {
  private final GeneralTestEventsProcessor myEventsProcessor;

  public BaseThriftTestHandler(GeneralTestEventsProcessor eventsProcessor) {
    myEventsProcessor = eventsProcessor;
  }

  @Override
  public void runStarted() throws TException {
    myEventsProcessor.onStartTesting();
  }

  @Override
  public void runFinished() throws TException {
    myEventsProcessor.onFinishTesting();
  }

  @Override
  public void suiteStarted(String name, String location) throws TException {
    myEventsProcessor.onSuiteStarted(new TestSuiteStartedEvent(name, location));
  }

  @Override
  public void suiteTestCount(int count) throws TException {
    myEventsProcessor.onTestsCountInSuite(count);
  }

  @Override
  public void suiteFinished(String name) throws TException {
    myEventsProcessor.onSuiteFinished(new TestSuiteFinishedEvent(name));
  }

  @Override
  public void testStarted(String name, String location) throws TException {
    myEventsProcessor.onTestStarted(new TestStartedEvent(name, location));
  }

  @Override
  public void testFailed(String name, String message, String trace, boolean testError, String actual, String expected) throws TException {
    myEventsProcessor.onTestFailure(new TestFailedEvent(name, message, trace, testError, actual, expected));
  }

  @Override
  public void testIgnored(String name, String comment, String trace) throws TException {
    myEventsProcessor.onTestIgnored(new TestIgnoredEvent(name, comment, trace));
  }

  @Override
  public void testOutput(String name, String text, boolean stdOut) throws TException {
    myEventsProcessor.onTestOutput(new TestOutputEvent(name, text, stdOut));
  }

  @Override
  public void testFinished(String name, long time) throws TException {
    myEventsProcessor.onTestFinished(new TestFinishedEvent(name, time));
  }
}
