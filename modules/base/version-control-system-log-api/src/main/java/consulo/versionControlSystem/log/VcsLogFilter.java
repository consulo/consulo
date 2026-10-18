package consulo.versionControlSystem.log;

/**
 * Marker interface indicating that this is a filter for the VCS log.
 */
public interface VcsLogFilter {
  /**
   * Text presentation of the filter, used for example in the title of a log tab.
   */
  String getDisplayText();
}
