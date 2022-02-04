package com.intellij.util.diff;

/**
 * @author irengrig
 */
public class FilesTooBigForDiffException extends Exception {
  // Do not try to compare files if difference is bigger than given threshold
  public static final int DELTA_THRESHOLD_SIZE = 20000;// Registry.intValue("diff.delta.threshold.size");

  // Do not try to compare two lines by-word after this much fails.
  public static final int MAX_BAD_LINES = 3;

  public FilesTooBigForDiffException() {
    super("Can not calculate diff. File is too big and there are too many changes.");
  }
}
