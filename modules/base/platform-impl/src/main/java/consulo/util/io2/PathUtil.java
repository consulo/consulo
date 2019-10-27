package consulo.util.io2;

import consulo.logging.Logger;
import javax.annotation.Nonnull;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.Set;

/**
 * @author VISTALL
 * @since 31-Oct-16
 */
public class PathUtil {
  public static final Logger LOGGER = Logger.getInstance(PathUtil.class);

  private static final boolean ourSupportPosixFilePermissions = FileSystems.getDefault().supportedFileAttributeViews().contains("posix");

  public static boolean isSupportPosixFilePermissions() {
    return ourSupportPosixFilePermissions;
  }

  public static void setPosixFilePermissions(@Nonnull Path path, @Nonnull Set<PosixFilePermission> posixFilePermissions) {
    if (isSupportPosixFilePermissions()) {
      try {
        Files.setPosixFilePermissions(path, posixFilePermissions);
      }
      catch (IOException e) {
        LOGGER.error(e);
      }
    }
  }

  @Nonnull
  public static Set<PosixFilePermission> convertModeToFilePermissions(int mode) {
    int mask = 1;
    Set<PosixFilePermission> perms = EnumSet.noneOf(PosixFilePermission.class);
    for (PosixFilePermission flag : PosixFilePermission.values()) {
      if (flag != null && (mask & mode) != 0) {
        perms.add(flag);
      }
      mask = mask << 1;
    }
    return perms;
  }
}
