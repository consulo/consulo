package consulo.externalStorage;

import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * @author VISTALL
 * @since 20-Feb-17
 */
public class ExternalStorageUtil {
  @NotNull
  public static File getModCountFile(@NotNull File baseFile) {
    return new File(baseFile.getParentFile(), baseFile.getName() + ".mod");
  }
}
