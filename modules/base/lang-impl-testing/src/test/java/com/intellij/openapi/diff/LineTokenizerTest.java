package com.intellij.openapi.diff;

import com.intellij.util.Assertion;
import org.junit.Assert;
import org.junit.Test;

public class LineTokenizerTest extends Assert {
  private final Assertion CHECK = new Assertion();

  @Test
  public void test() {
    CHECK.compareAll(new String[]{"a\n", "b\n", "c\n", "d\n"}, new LineTokenizer("a\nb\n\rc\rd\r\n").execute());
    CHECK.compareAll(new String[]{"a\n", "b"}, new LineTokenizer("a\nb").execute());
    LineTokenizer lineTokenizer = new LineTokenizer("a\n\r\r\nb");
    CHECK.compareAll(new String[]{"a\n", "\n", "b"}, lineTokenizer.execute());
    assertEquals("\n\r", lineTokenizer.getLineSeparator());
  }
}
