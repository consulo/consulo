package consulo.components.impl.stores;

import com.intellij.openapi.components.RoamingType;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;

public abstract class StreamProvider {
  public static final StreamProvider[] EMPTY_ARRAY = new StreamProvider[0];

  public boolean isEnabled() {
    return true;
  }

  /**
   * fileSpec Only main fileSpec, not version
   */
  public boolean isApplicable(@Nonnull String fileSpec, @Nonnull RoamingType roamingType) {
    return true;
  }

  /**
   * @param fileSpec
   * @param content bytes of content
   * @param roamingType
   */
  public abstract void saveContent(@Nonnull String fileSpec, @Nonnull byte[] content, @Nonnull RoamingType roamingType) throws IOException;

  @Nullable
  public abstract InputStream loadContent(@Nonnull String fileSpec, @Nonnull RoamingType roamingType) throws IOException;

  @Nonnull
  public Collection<String> listSubFiles(@Nonnull String fileSpec, @Nonnull RoamingType roamingType) {
    return Collections.emptyList();
  }

  /**
   * Delete file or directory
   */
  public abstract void delete(@Nonnull String fileSpec, @Nonnull RoamingType roamingType);
}