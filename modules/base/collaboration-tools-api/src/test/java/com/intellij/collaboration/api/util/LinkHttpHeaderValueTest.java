// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.api.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class LinkHttpHeaderValueTest {
  @Test
  public void testCorrectHeader() {
    LinkHttpHeaderValue result = LinkHttpHeaderValue.parse(
      "<https://api.github.com/search/code?q=addClass+user%3Amozilla&page=15>; rel=\"next\", " +
      "<https://api.github.com/search/code?q=addClass+user%3Amozilla&page=34>; rel=\"last\", " +
      "<https://api.github.com/search/code?q=addClass+user%3Amozilla&page=1>; rel=\"first\", " +
      "<https://api.github.com/search/code?q=addClass+user%3Amozilla&page=13>; rel=\"prev\"");

    assertEquals(new LinkHttpHeaderValue(
      "https://api.github.com/search/code?q=addClass+user%3Amozilla&page=1",
      "https://api.github.com/search/code?q=addClass+user%3Amozilla&page=13",
      "https://api.github.com/search/code?q=addClass+user%3Amozilla&page=15",
      "https://api.github.com/search/code?q=addClass+user%3Amozilla&page=34",
      false
    ), result);
  }

  @Test
  public void testEmptyHeader() {
    LinkHttpHeaderValue result = LinkHttpHeaderValue.parse("");

    assertEquals(new LinkHttpHeaderValue(), result);
  }

  @Test
  public void testPartialHeader() {
    LinkHttpHeaderValue result = LinkHttpHeaderValue.parse(
      "<https://api.github.com/search/code?q=addClass+user%3Amozilla&page=34>; rel=\"last\", " +
      "<https://api.github.com/search/code?q=addClass+user%3Amozilla&page=1>; rel=\"first\", " +
      "<https://api.github.com/search/code?q=addClass+user%3Amozilla&page=33>; rel=\"prev\"");

    assertEquals(new LinkHttpHeaderValue(
      "https://api.github.com/search/code?q=addClass+user%3Amozilla&page=1",
      "https://api.github.com/search/code?q=addClass+user%3Amozilla&page=33",
      null,
      "https://api.github.com/search/code?q=addClass+user%3Amozilla&page=34",
      false
    ), result);
  }

  @Test
  public void deprecationIsAccepted() {
    LinkHttpHeaderValue result = LinkHttpHeaderValue.parse(
      "<>; rel=\"deprecation\""
    );

    assertEquals(new LinkHttpHeaderValue(
      null, null, null, null, true
    ), result);
  }

  @Test
  public void deprecationIsAcceptedAndTypeIsIgnored() {
    LinkHttpHeaderValue result = LinkHttpHeaderValue.parse(
      "<>; rel=\"deprecation\"; type=\"text/html\""
    );

    assertEquals(new LinkHttpHeaderValue(
      null, null, null, null, true
    ), result);
  }

  @Test
  public void firstRelValueIsUsed() {
    LinkHttpHeaderValue result = LinkHttpHeaderValue.parse(
      "<bla>; rel=\"first\"; rel=\"last\""
    );

    assertEquals(new LinkHttpHeaderValue(
      "bla", null, null, null, false
    ), result);
  }

  @Test
  public void relValueIsUsedAlsoWhenQuotationMarksAreMissing() {
    LinkHttpHeaderValue result = LinkHttpHeaderValue.parse(
      "<bla>; rel=first"
    );

    assertEquals(new LinkHttpHeaderValue(
      "bla", null, null, null, false
    ), result);
  }

  @Test(expected = IllegalStateException.class)
  public void testIncorrectHeader() {
    LinkHttpHeaderValue.parse("abirvalg");
  }
}
