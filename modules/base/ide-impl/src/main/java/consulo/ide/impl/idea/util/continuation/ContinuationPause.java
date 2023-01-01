package consulo.ide.impl.idea.util.continuation;

/**
 * @author irengrig
 *         Date: 6/28/11
 *         Time: 3:26 PM
 */
public interface ContinuationPause {
  void suspend();

  void ping();
}
