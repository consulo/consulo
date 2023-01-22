package consulo.remoteServer.configuration;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicAPI;

import javax.annotation.Nonnull;
import java.util.EventListener;

/**
 * @author nik
 */
@TopicAPI(ComponentScope.APPLICATION)
public interface RemoteServerListener extends EventListener {
  void serverAdded(@Nonnull RemoteServer<?> server);

  void serverRemoved(@Nonnull RemoteServer<?> server);
}
