package consulo.ui.desktop.internal.image.canvas;

/**
 * mxUtils
 * Based on jgraphx, license BSD
 */
class DesktopCanvas2DImplUtil {
  /**
   * Convert a string representing a dash pattern into a float array.
   * A valid dash pattern is a string of dash widths (floating point values)
   * separated by space characters.
   *
   * @param dashPatternString the string representing the dash pattern
   * @return float[]
   * @throws NumberFormatException if any of the dash widths cannot be interpreted as a
   *                               floating point number
   */
  public static float[] parseDashPattern(String dashPatternString) throws NumberFormatException {
    if (dashPatternString != null && dashPatternString.length() > 0) {
      String[] tokens = dashPatternString.split(" ");
      float[] dashpattern = new float[tokens.length];
      float dashWidth;

      for (int i = 0; i < tokens.length; i++) {
        dashWidth = (float)(Float.parseFloat(tokens[i]));

        if (dashWidth > 0) {
          dashpattern[i] = dashWidth;
        }
        else {
          throw new NumberFormatException("Dash width must be positive");
        }
      }

      return dashpattern;
    }
    return null;
  }
}
