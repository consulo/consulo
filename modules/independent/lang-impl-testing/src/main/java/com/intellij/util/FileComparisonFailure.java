package com.intellij.util;

import junit.framework.ComparisonFailure;

/**
 * @author VISTALL
 * @since 15:48/02.06.13
 */
public class FileComparisonFailure extends ComparisonFailure {
  private final String myExpected;
  private final String myActual;
  private final String myFilePath;

  public FileComparisonFailure(String message, String expected, String actual, String filePath) {
    super(message, expected, actual);
    myExpected = expected;
    myActual = actual;
    myFilePath = filePath;
  }

  public String getFilePath() {
    return myFilePath;
  }

  @Override
  public String getExpected() {
    return myExpected;
  }

  @Override
  public String getActual() {
    return myActual;
  }
}
