package consulo.ide.impl.idea.remote;

/**
 * @author traff
 */
public interface RemoteSdkCredentials extends MutableRemoteCredentials, RemoteSdkProperties {
  String getFullInterpreterPath();
}
