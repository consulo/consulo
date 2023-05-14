package consulo.versionControlSystem.log;

import jakarta.annotation.Nonnull;

/**
 * Filter which needs {@link VcsCommitMetadata} to work.
 *
 * @see VcsLogGraphFilter
 */
public interface VcsLogDetailsFilter extends VcsLogFilter {

  boolean matches(@Nonnull VcsCommitMetadata details);
}
