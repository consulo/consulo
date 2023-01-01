package consulo.ide.impl.idea.remote;

import consulo.process.remote.RemoteSdkException;

/**
 * @author traff
 */
public class RemoteCancelledException extends RemoteSdkException {
  public RemoteCancelledException(String s) {
    super(s);
  }
}
