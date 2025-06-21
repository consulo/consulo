package consulo.util.io;

import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.Set;

/**
 * @author VISTALL
 * @since 31-Oct-16
 */
public class NioPathUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(NioPathUtil.class);

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
                LOGGER.error(path.toString(), e);
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
