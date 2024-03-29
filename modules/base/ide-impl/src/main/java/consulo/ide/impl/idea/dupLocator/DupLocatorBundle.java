package consulo.ide.impl.idea.dupLocator;

import consulo.application.CommonBundle;
import org.jetbrains.annotations.NonNls;
import jakarta.annotation.Nonnull;
import org.jetbrains.annotations.PropertyKey;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.ResourceBundle;

public class DupLocatorBundle {

  public static String message(@Nonnull @PropertyKey(resourceBundle = BUNDLE) String key, @Nonnull Object... params) {
    return CommonBundle.message(getBundle(), key, params);
  }

  private static Reference<ResourceBundle> ourBundle;
  @NonNls private static final String BUNDLE = "messages.DupLocatorBundle";

  private DupLocatorBundle() {
  }

  private static ResourceBundle getBundle() {
    ResourceBundle bundle = consulo.ide.impl.idea.reference.SoftReference.dereference(ourBundle);
    if (bundle == null) {
      bundle = ResourceBundle.getBundle(BUNDLE);
      ourBundle = new SoftReference<>(bundle);
    }
    return bundle;
  }
}
