package consulo.ui.web.servlet;

import com.intellij.util.containers.ContainerUtil;
import consulo.ui.image.Image;
import consulo.ui.web.internal.image.WebImageWithVaadinState;

import javax.annotation.Nonnull;
import java.net.URL;
import java.util.Map;

/**
 * @author VISTALL
 * @since 18-Jan-17
 */
public class WebImageUrlCache {
  public static final Map<Integer, URL> ourURLCache = ContainerUtil.newConcurrentMap();

  public static WebImageWithVaadinState map(Image other) {
    return (WebImageWithVaadinState)other;
  }

  @Nonnull
  public static String createURL(int hash, String prefix) {
    return prefix + "/image?urlHash=\"" + hash + "\"";
  }

  public static int hashCode(URL url) {
    int i = url.hashCode();
    ourURLCache.putIfAbsent(i, url);
    return i;
  }
}
