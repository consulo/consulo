package consulo.content.bundle;

import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2018-06-22
 */
public interface BundleHolder {
  static BundleHolder EMPTY = () -> Sdk.EMPTY_ARRAY;

  
  Sdk[] getBundles();

  default void forEachBundle(Consumer<Sdk> sdkConsumer) {
    for (Sdk sdk : getBundles()) {
      sdkConsumer.accept(sdk);
    }
  }
}
