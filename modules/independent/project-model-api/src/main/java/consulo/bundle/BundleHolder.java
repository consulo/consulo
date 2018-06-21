package consulo.bundle;

import com.intellij.openapi.projectRoots.Sdk;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2018-06-22
 */
public interface BundleHolder {
  static BundleHolder EMPTY = () -> Sdk.EMPTY_ARRAY;

  @Nonnull
  Sdk[] getBundles();
}
