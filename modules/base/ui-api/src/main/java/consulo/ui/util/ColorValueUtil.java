package consulo.ui.util;

import com.intellij.openapi.util.text.StringUtilRt;
import consulo.annotation.UsedInPlugin;
import consulo.ui.shared.RGBColor;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2018-06-12
 */
@UsedInPlugin
public class ColorValueUtil {
  /**
   * Return Color object from string. The following formats are allowed:
   * <code>#abc123</code>,
   * <code>ABC123</code>,
   * <code>ab5</code>,
   * <code>#FFF</code>.
   *
   * @param str hex string
   * @return RGBColor object
   */
  @Nonnull
  public static RGBColor fromHex(@Nonnull String str) {
    str = StringUtilRt.trimStart(str, "#");
    if (str.length() == 3) {
      return new RGBColor(17 * Integer.valueOf(String.valueOf(str.charAt(0)), 16), 17 * Integer.valueOf(String.valueOf(str.charAt(1)), 16), 17 * Integer.valueOf(String.valueOf(str.charAt(2)), 16));
    }
    else if (str.length() == 6) {
      return RGBColor.decode("0x" + str);
    }
    else {
      throw new IllegalArgumentException("Should be String of 3 or 6 chars length.");
    }
  }
}
