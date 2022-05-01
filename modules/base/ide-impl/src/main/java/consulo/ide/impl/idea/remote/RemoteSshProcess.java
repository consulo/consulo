package consulo.ide.impl.idea.remote;

import consulo.process.SelfKiller;

/**
 * @author traff
 */
abstract public class RemoteSshProcess extends RemoteProcess implements SelfKiller, Tunnelable {
  /**
   * @deprecated use {@link #killProcessTree()}
   */
  @Deprecated
  protected abstract boolean hasPty();

  /**
   * @deprecated use {@link #killProcessTree()}
   */
  @Deprecated
  protected abstract boolean sendCtrlC();

  public boolean killProcessTree() {
    if (hasPty()) {
      return sendCtrlC();
    }
    else {
      return false;
    }
  }
}
