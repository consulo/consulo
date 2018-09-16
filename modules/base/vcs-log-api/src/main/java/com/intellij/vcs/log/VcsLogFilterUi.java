package com.intellij.vcs.log;

import javax.annotation.Nonnull;

/**
 * Graphical UI for filtering commits in the log.
 */
public interface VcsLogFilterUi {

  /**
   * Returns the filters currently active, i.e. switched on by user.
   */
  @Nonnull
  VcsLogFilterCollection getFilters();

  /**
   * Sets the given filter to the given value and updates the log view. <br/>
   * <b>Note:</b> only VcsLogBranchFilter is currently supported.
   */
  void setFilter(@Nonnull VcsLogFilter filter);
}
