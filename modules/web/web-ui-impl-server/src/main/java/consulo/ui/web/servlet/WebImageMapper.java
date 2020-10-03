package consulo.ui.web.servlet;

import consulo.ui.image.Image;
import consulo.ui.web.internal.image.WebImageWithVaadinState;

import javax.annotation.Nonnull;
import java.net.URL;

/**
 * @author VISTALL
 * @since 18-Jan-17
 */
public class WebImageMapper {
  public static WebImageWithVaadinState map(Image other) {
    return (WebImageWithVaadinState)other;
  }

  @Nonnull
  public static String createURL(int hash, String prefix) {
    return prefix + "/image?urlHash=\"" + hash + "\"";
  }

  public static int hashCode(URL url) {
    int i = url.hashCode();
    return i;
  }
}
