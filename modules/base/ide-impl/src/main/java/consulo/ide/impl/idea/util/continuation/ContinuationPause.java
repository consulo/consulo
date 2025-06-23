package consulo.ide.impl.idea.util.continuation;

/**
 * @author irengrig
 * @since 2011-06-28
 */
public interface ContinuationPause {
  void suspend();

  void ping();
}
