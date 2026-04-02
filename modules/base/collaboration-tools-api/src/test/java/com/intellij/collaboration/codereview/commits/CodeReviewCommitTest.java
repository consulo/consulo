// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.codereview.commits;

import com.intellij.collaboration.ui.codereview.commits.CommitsUtil;
import kotlin.Pair;
import org.junit.Test;

import static kotlin.test.AssertionsKt.assertEquals;

final class CodeReviewCommitTest {
  @Test
  public void parseCommitMessageTitleWithoutDescription() {
    String expectedTitle = "Commit title";
    String expectedDescription = "";

    String commitMessage = expectedTitle;

    Pair<String, String> result = CommitsUtil.splitCommitMessage(commitMessage);
    assertEquals(expectedTitle, result.getFirst(), null);
    assertEquals(expectedDescription, result.getSecond(), null);
  }

  @Test
  public void parseCommitMessageTitleWithDescription() {
    String expectedTitle = "Commit title";
    String expectedDescription = "* fixed: commit description";

    String commitMessage = expectedTitle + "\n\n" + expectedDescription;

    Pair<String, String> result = CommitsUtil.splitCommitMessage(commitMessage);
    assertEquals(expectedTitle, result.getFirst(), null);
    assertEquals(expectedDescription, result.getSecond(), null);
  }

  @Test
  public void parseCommitMessageTitleWithLongDescription() {
    String expectedTitle = "Commit title";
    String expectedDescription =
      "* fixed1: commit description1\n" +
      "* fixed2: commit description2\n" +
      "* fixed3: commit description3";

    String commitMessage = expectedTitle + "\n\n" + expectedDescription;

    Pair<String, String> result = CommitsUtil.splitCommitMessage(commitMessage);
    assertEquals(expectedTitle, result.getFirst(), null);
    assertEquals(expectedDescription, result.getSecond(), null);
  }

  @Test
  public void parseCommitMessageTitleWithEmptyDescription() {
    String expectedTitle = "Commit title";
    String customDescription = " ";

    String commitMessage = expectedTitle + "\n\n" + customDescription;

    Pair<String, String> result = CommitsUtil.splitCommitMessage(commitMessage);
    assertEquals(expectedTitle, result.getFirst(), null);
    assertEquals("", result.getSecond(), null);
  }
}
