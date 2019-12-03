/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package com.intellij.execution.testframework.sm.runner;

import com.intellij.execution.testframework.sm.runner.events.*;
import com.intellij.openapi.project.Project;
import consulo.util.dataholder.Key;
import javax.annotation.Nonnull;

/**
* @author Roman.Chernyatchik
*/
public class MockGeneralTestEventsProcessorAdapter extends GeneralTestEventsProcessor {
  private final StringBuilder myOutputBuffer = new StringBuilder();

  public MockGeneralTestEventsProcessorAdapter(Project project, @Nonnull String testFrameworkName, @Nonnull SMTestProxy.SMRootTestProxy testsRootProxy) {
    super(project, testFrameworkName, testsRootProxy);
  }

  @Override
  public void onStartTesting() {
  }

  @Override
  public void onTestsCountInSuite(int count) {
  }

  @Override
  public void onTestStarted(@Nonnull TestStartedEvent testStartedEvent) {
  }

  @Override
  public void onTestFinished(@Nonnull TestFinishedEvent testFinishedEvent) {
  }

  @Override
  public void onTestFailure(@Nonnull TestFailedEvent testFailedEvent) {
  }

  @Override
  public void onTestIgnored(@Nonnull TestIgnoredEvent testIgnoredEvent) {
  }

  @Override
  public void onTestOutput(@Nonnull TestOutputEvent testOutputEvent) {
  }

  @Override
  public void onSuiteStarted(@Nonnull TestSuiteStartedEvent suiteStartedEvent) {
  }

  @Override
  public void onSuiteFinished(@Nonnull TestSuiteFinishedEvent suiteFinishedEvent) {
  }

  @Override
  public void onUncapturedOutput(@Nonnull String text, Key outputType) {
    myOutputBuffer.append("[").append(outputType.toString()).append("]").append(text);
  }

  @Override
  public void onError(@Nonnull String localizedMessage, @javax.annotation.Nullable String stackTrace, boolean isCritical) {
  }

  @Override
  public void onCustomProgressTestsCategory(@javax.annotation.Nullable String categoryName, int testCount) {
  }

  @Override
  public void onCustomProgressTestStarted() {
  }

  @Override
  public void onCustomProgressTestFailed() {
  }

  @Override
  public void onTestsReporterAttached() {
  }

  @Override
  public void setLocator(@Nonnull SMTestLocator locator) {

  }

  @Override
  public void addEventsListener(@Nonnull SMTRunnerEventsListener viewer) {
  }

  @Override
  public void onFinishTesting() {
  }

  @Override
  public void setPrinterProvider(@Nonnull TestProxyPrinterProvider printerProvider) {
  }

  @Override
  public void dispose() {
    myOutputBuffer.setLength(0);
  }

  public String getOutput() {
    return myOutputBuffer.toString();
  }
}
