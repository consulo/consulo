package consulo.ide.impl.idea.remote;

import com.google.common.net.HostAndPort;
import consulo.ide.impl.idea.util.PathMapper;
import consulo.ide.impl.idea.util.PathMappingSettings;
import consulo.process.remote.RemoteSdkException;
import consulo.util.lang.Pair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.List;

/**
 * @author traff
 */
public interface RemoteProcessControl {
  @Nonnull
  PathMapper getMappingSettings();

  /**
   * @deprecated use {@link #getRemoteSocket(int)}
   */
  @Deprecated
  void addRemoteForwarding(int remotePort, int localPort);

  Pair<String, Integer> getRemoteSocket(int localPort) throws RemoteSdkException;

  @Nullable
  HostAndPort getLocalTunnel(int remotePort);

  List<PathMappingSettings.PathMapping> getFileMappings();
}
