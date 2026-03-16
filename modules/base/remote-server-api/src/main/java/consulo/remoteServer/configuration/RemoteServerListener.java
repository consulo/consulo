package consulo.remoteServer.configuration;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicAPI;

import java.util.EventListener;

/**
 * @author nik
 */
@TopicAPI(ComponentScope.APPLICATION)
public interface RemoteServerListener extends EventListener {
  void serverAdded(RemoteServer<?> server);

  void serverRemoved(RemoteServer<?> server);
}
