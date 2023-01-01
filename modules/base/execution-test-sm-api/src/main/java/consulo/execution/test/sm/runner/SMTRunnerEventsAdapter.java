/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.execution.test.sm.runner;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author Roman Chernyatchik
 */
public class SMTRunnerEventsAdapter implements SMTRunnerEventsListener {
  @Override
  public void onTestingStarted(@Nonnull SMTestProxy.SMRootTestProxy testsRoot) {
  }

  @Override
  public void onTestingFinished(@Nonnull SMTestProxy.SMRootTestProxy testsRoot) {
  }

  @Override
  public void onTestsCountInSuite(final int count) {
  }

  @Override
  public void onTestStarted(@Nonnull final SMTestProxy test) {
  }

  @Override
  public void onTestFinished(@Nonnull final SMTestProxy test) {
  }

  @Override
  public void onTestFailed(@Nonnull final SMTestProxy test) {
  }

  @Override
  public void onTestIgnored(@Nonnull final SMTestProxy test) {
  }

  @Override
  public void onSuiteStarted(@Nonnull final SMTestProxy suite) {
  }

  @Override
  public void onSuiteFinished(@Nonnull final SMTestProxy suite) {
  }

  // Custom progress status

  @Override
  public void onCustomProgressTestsCategory(@Nullable String categoryName, final int testCount) {
  }

  @Override
  public void onCustomProgressTestStarted() {
  }

  @Override
  public void onCustomProgressTestFailed() {
  }

  @Override
  public void onCustomProgressTestFinished() {
  }

  @Override
  public void onSuiteTreeNodeAdded(SMTestProxy testProxy) {
  }

  @Override
  public void onSuiteTreeStarted(SMTestProxy suite) {
  }
}
