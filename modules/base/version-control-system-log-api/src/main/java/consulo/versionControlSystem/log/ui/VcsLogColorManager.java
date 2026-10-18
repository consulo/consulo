package consulo.versionControlSystem.log.ui;

import consulo.virtualFileSystem.VirtualFile;

import consulo.ui.color.ColorValue;

/**
 * Managers colors used for the vcs log: references, roots, branches, etc.
 *
 * @author Kirill Likhodedov
 */
public interface VcsLogColorManager {

  /**
   * Returns the color assigned to the given repository root.
   */
  ColorValue getRootColor(VirtualFile root);

  /**
   * Tells if there are several repositories currently shown in the log.
   */
  boolean isMultipleRoots();
}
