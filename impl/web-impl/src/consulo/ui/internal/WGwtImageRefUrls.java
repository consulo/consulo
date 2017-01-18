package consulo.ui.internal;

import com.intellij.util.containers.ContainerUtil;
import consulo.ui.ImageRef;
import consulo.ui.migration.SwingImageRef;

import java.net.URL;
import java.util.concurrent.ConcurrentMap;

/**
 * @author VISTALL
 * @since 18-Jan-17
 */
public class WGwtImageRefUrls {
  public static final ConcurrentMap<Integer, URL> ourURLCache = ContainerUtil.newConcurrentMap();

  public static String getUrlForBrowser(ImageRef imageRef) {
    int hashCode;
    if (imageRef instanceof WGwtImageRefImpl) {
      hashCode = ((WGwtImageRefImpl)imageRef).getHashCode();
    }
    else if(imageRef instanceof SwingImageRef) {
      URL iconURL = ((SwingImageRef)imageRef).getIconURL();
      ourURLCache.putIfAbsent(hashCode = hashCode(iconURL), iconURL);
    }
    else {
      throw new UnsupportedOperationException("unsupported image ref");
    }
    return "/uiIcon?urlHash=\"" + hashCode + "\"";
  }

  public static int hashCode(URL url) {
    return url.hashCode();
  }
}
