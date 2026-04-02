// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.codereview.issues;

import com.intellij.collaboration.ui.codereview.issues.IssueIdsProcessor;
import com.intellij.openapi.vcs.IssueNavigationConfiguration;
import com.intellij.openapi.vcs.IssueNavigationLink;
import com.intellij.testFramework.HeavyPlatformTestCase;

import java.util.List;

import static org.junit.Assert.assertEquals;

final class CodeReviewIssueIdsProcessorTest extends HeavyPlatformTestCase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    // setup youtrack issues handling like IDEA-1234
    IssueNavigationConfiguration.getInstance(getProject()).setLinks(List.of(
      new IssueNavigationLink("[A-Z]+\\-\\d+", "https://youtrack.jetbrains.com/issue/$0")
    ));
  }

  // IDEA-261255
  public void testYoutrackIssueSubstitute() {
    String issueId = "IDEA-1234";
    String markdown = "Check youtrack issue: " + issueId + ". Should be link";

    shouldBeConvertedTo(markdown,
      "Check youtrack issue: [" + issueId + "](https://youtrack.jetbrains.com/issue/" + issueId + "). Should be link");
  }

  // IDEA-278800
  public void testMarkdownWithLinksShouldStaySame() {
    String markdown = "Link [hello](https://google.com) should be the same";

    shouldBeConvertedTo(markdown, markdown);
  }

  public void testMarkdownWithLinksAndYoutrackIssue() {
    String issueId = "IDEA-1234";
    String markdown = "Link [hello](https://google.com). Youtrack issue: " + issueId;

    shouldBeConvertedTo(markdown,
      "Link [hello](https://google.com). Youtrack issue: [" + issueId + "](https://youtrack.jetbrains.com/issue/" + issueId + ")");
  }

  public void testLinkWithIssueInsideShouldStaySame() {
    String issueId = "IDEA-1234";
    String markdown = "Link with issue inside: [hello](https://google.com/" + issueId + ")";

    shouldBeConvertedTo(markdown, markdown);
  }

  // TODO: fix IDEA-280335
  public void testMarkdownLinkNameWithIssueInsideShouldStaySame() {
    String issueId = "IDEA-1234";
    String markdown = "Link with issue inside: [Issue: " + issueId + "](https://google.com)";

    shouldBeConvertedTo(markdown, markdown);
  }

  private void shouldBeConvertedTo(String markdown, String expected) {
    String actual = IssueIdsProcessor.processIssueIdsMarkdown(getProject(), markdown);
    assertEquals(expected, actual);
  }
}
