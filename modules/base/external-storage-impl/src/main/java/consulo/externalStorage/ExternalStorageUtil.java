package consulo.externalStorage;

import javax.annotation.Nonnull;

import java.io.File;

/**
 * @author VISTALL
 * @since 20-Feb-17
 */
public class ExternalStorageUtil {
  @Nonnull
  public static File getModCountFile(@Nonnull File baseFile) {
    return new File(baseFile.getParentFile(), baseFile.getName() + ".mod");
  }
}
