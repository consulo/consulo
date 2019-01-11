package consulo.ui.web.internal.image;

import com.intellij.util.containers.ContainerUtil;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import java.net.URL;
import java.util.Map;

/**
 * @author VISTALL
 * @since 18-Jan-17
 */
public class WGwtImageUrlCache {
  public static final Map<Integer, URL> ourURLCache = ContainerUtil.newConcurrentMap();

  public static WGwtImageWithState map(Image other) {
    return (WGwtImageWithState)other;
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
