package consulo.component.store.internal;

import consulo.component.persist.RoamingType;
import org.jspecify.annotations.Nullable;

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
  public boolean isApplicable(String fileSpec, RoamingType roamingType) {
    return true;
  }

  /**
   * @param fileSpec
   * @param content bytes of content
   * @param roamingType
   */
  public abstract void saveContent(String fileSpec, byte[] content, RoamingType roamingType) throws IOException;

  public abstract @Nullable InputStream loadContent(String fileSpec, RoamingType roamingType) throws IOException;

  
  public Collection<String> listSubFiles(String fileSpec, RoamingType roamingType) {
    return Collections.emptyList();
  }

  /**
   * Delete file or directory
   */
  public abstract void delete(String fileSpec, RoamingType roamingType);
}