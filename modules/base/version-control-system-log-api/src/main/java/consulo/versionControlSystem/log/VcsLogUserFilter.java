package consulo.versionControlSystem.log;

import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;
import java.util.Collection;

/**
 * Filters commits by one or several users.
 */
public interface VcsLogUserFilter extends VcsLogDetailsFilter {

  /**
   * Returns users selected in the filter, concerning the passed VCS root.
   *
   * @param root has no effect if user chooses some user name;
   *             it is needed if user selects the predefined value "me" which means the current user.
   *             Since current user name can be defined differently for different roots, we pass the root for which this value is
   *             requested.
   */
  @Nonnull
  Collection<VcsUser> getUsers(@Nonnull VirtualFile root);
}
