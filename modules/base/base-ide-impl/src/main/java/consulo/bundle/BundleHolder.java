package consulo.bundle;

import com.intellij.openapi.projectRoots.Sdk;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2018-06-22
 */
public interface BundleHolder {
  static BundleHolder EMPTY = () -> Sdk.EMPTY_ARRAY;

  @Nonnull
  Sdk[] getBundles();

  default void forEachBundle(@Nonnull Consumer<Sdk> sdkConsumer) {
    for (Sdk sdk : getBundles()) {
      sdkConsumer.accept(sdk);
    }
  }
}
