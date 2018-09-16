/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.execution.testframework.sm.runner.events;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import jetbrains.buildServer.messages.serviceMessages.TestFailed;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class TestFailedEvent extends TreeNodeEvent {

  private final String myLocalizedFailureMessage;
  private final String myStacktrace;
  private final boolean myTestError;
  private final String myComparisonFailureActualText;
  private final String myComparisonFailureExpectedText;
  private final String myExpectedFilePath;
  private final String myActualFilePath;
  private final long myDurationMillis;
  private boolean myExpectedFileTemp;
  private boolean myActualFileTemp;

  public TestFailedEvent(@Nonnull TestFailed testFailed, boolean testError) {
    this(testFailed, testError, null);
  }
  public TestFailedEvent(@Nonnull TestFailed testFailed, boolean testError, @javax.annotation.Nullable String expectedFilePath) {
    this(testFailed, testError, expectedFilePath, null);
  }
  public TestFailedEvent(@Nonnull TestFailed testFailed,
                         boolean testError,
                         @Nullable String expectedFilePath,
                         @javax.annotation.Nullable String actualFilePath) {
    super(testFailed.getTestName(), TreeNodeEvent.getNodeId(testFailed));
    if (testFailed.getFailureMessage() == null) throw new NullPointerException();
    myLocalizedFailureMessage = testFailed.getFailureMessage();
    myStacktrace = testFailed.getStacktrace();
    myTestError = testError;

    myExpectedFilePath = expectedFilePath;
    String expected = testFailed.getExpected();
    if (expected == null && expectedFilePath != null) {
      try {
        expected = FileUtil.loadFile(new File(expectedFilePath));
      }
      catch (IOException ignore) {}
    }
    myComparisonFailureExpectedText = expected;

    myActualFilePath = actualFilePath;
    String actual = testFailed.getActual();
    if (actual == null && actualFilePath != null) {
      try {
        actual = FileUtil.loadFile(new File(actualFilePath));
      }
      catch (IOException ignore) {}
    }
    myComparisonFailureActualText = actual;

    Map<String, String> attributes = testFailed.getAttributes();
    myDurationMillis = parseDuration(attributes.get("duration"));
    myActualFileTemp = Boolean.parseBoolean(attributes.get("actualIsTempFile"));
    myExpectedFileTemp = Boolean.parseBoolean(attributes.get("expectedIsTempFile"));
  }

  public boolean isExpectedFileTemp() {
    return myExpectedFileTemp;
  }

  public boolean isActualFileTemp() {
    return myActualFileTemp;
  }

  private static long parseDuration(@javax.annotation.Nullable String durationStr) {
    if (!StringUtil.isEmpty(durationStr)) {
      try {
        return Long.parseLong(durationStr);
      }
      catch (NumberFormatException ignored) {
      }
    }
    return -1;
  }

  public TestFailedEvent(@Nonnull String testName,
                         @Nonnull String localizedFailureMessage,
                         @javax.annotation.Nullable String stackTrace,
                         boolean testError,
                         @javax.annotation.Nullable String comparisonFailureActualText,
                         @Nullable String comparisonFailureExpectedText) {
    this(testName,
         null,
         localizedFailureMessage,
         stackTrace,
         testError,
         comparisonFailureActualText,
         comparisonFailureExpectedText,
         null,
         null,
         false,
         false,
         -1);
  }

  public TestFailedEvent(@javax.annotation.Nullable String testName,
                         @javax.annotation.Nullable String id,
                         @Nonnull String localizedFailureMessage,
                         @Nullable String stackTrace,
                         boolean testError,
                         @Nullable String comparisonFailureActualText,
                         @Nullable String comparisonFailureExpectedText,
                         @javax.annotation.Nullable String expectedFilePath,
                         @javax.annotation.Nullable String actualFilePath,
                         boolean expectedFileTemp,
                         boolean actualFileTemp,
                         long durationMillis) {
    super(testName, id);
    myLocalizedFailureMessage = localizedFailureMessage;
    myStacktrace = stackTrace;
    myTestError = testError;
    myExpectedFilePath = expectedFilePath;
    if (comparisonFailureExpectedText == null && expectedFilePath != null) {
      try {
        comparisonFailureExpectedText = FileUtil.loadFile(new File(expectedFilePath));
      }
      catch (IOException ignore) {}
    }
    myComparisonFailureActualText = comparisonFailureActualText;

    myActualFilePath = actualFilePath;
    myComparisonFailureExpectedText = comparisonFailureExpectedText;
    myDurationMillis = durationMillis;
    myExpectedFileTemp = expectedFileTemp;
    myActualFileTemp = actualFileTemp;
  }

  @Nonnull
  public String getLocalizedFailureMessage() {
    return myLocalizedFailureMessage;
  }

  @javax.annotation.Nullable
  public String getStacktrace() {
    return myStacktrace;
  }

  public boolean isTestError() {
    return myTestError;
  }

  @Nullable
  public String getComparisonFailureActualText() {
    return myComparisonFailureActualText;
  }

  @javax.annotation.Nullable
  public String getComparisonFailureExpectedText() {
    return myComparisonFailureExpectedText;
  }

  @Override
  protected void appendToStringInfo(@Nonnull StringBuilder buf) {
    append(buf, "localizedFailureMessage", myLocalizedFailureMessage);
    append(buf, "stacktrace", myStacktrace);
    append(buf, "isTestError", myTestError);
    append(buf, "comparisonFailureActualText", myComparisonFailureActualText);
    append(buf, "comparisonFailureExpectedText", myComparisonFailureExpectedText);
  }

  /**
   * @deprecated use {@link #getExpectedFilePath()} instead
   */
  public String getFilePath() {
    return myExpectedFilePath;
  }

  @javax.annotation.Nullable
  public String getExpectedFilePath() {
    return myExpectedFilePath;
  }

  @javax.annotation.Nullable
  public String getActualFilePath() {
    return myActualFilePath;
  }

  /**
   * @return the test duration in milliseconds, or -1 if undefined
   */
  public long getDurationMillis() {
    return myDurationMillis;
  }
}
