package com.intellij.vcs.log;

import com.intellij.openapi.vfs.VirtualFile;
import javax.annotation.Nonnull;

/**
 * Tells the VCS Log, that some data has possibly become obsolete and needs to be refreshed.
 *
 * @author Kirill Likhodedov
 */
public interface VcsLogRefresher {

  /**
   * Makes the log perform refresh for the given root.
   * This refresh can be optimized, i. e. it can query VCS just for the part of the log.
   */
  void refresh(@Nonnull VirtualFile root);
}
