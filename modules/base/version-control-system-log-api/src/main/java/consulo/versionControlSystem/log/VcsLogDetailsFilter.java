package consulo.versionControlSystem.log;


/**
 * Filter which needs {@link VcsCommitMetadata} to work.
 *
 * @see VcsLogGraphFilter
 */
public interface VcsLogDetailsFilter extends VcsLogFilter {

  boolean matches(VcsCommitMetadata details);
}
