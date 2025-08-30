package consulo.versionControlSystem.log.ui;

import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

import java.awt.*;

/**
 * Managers colors used for the vcs log: references, roots, branches, etc.
 *
 * @author Kirill Likhodedov
 */
public interface VcsLogColorManager {

  /**
   * Returns the color assigned to the given repository root.
   */
  @Nonnull
  Color getRootColor(@Nonnull VirtualFile root);

  /**
   * Tells if there are several repositories currently shown in the log.
   */
  boolean isMultipleRoots();
}
