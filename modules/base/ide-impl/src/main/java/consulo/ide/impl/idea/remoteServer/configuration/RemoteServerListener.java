package consulo.ide.impl.idea.remoteServer.configuration;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.Topic;

import javax.annotation.Nonnull;
import java.util.EventListener;

/**
 * @author nik
 */
@Topic(ComponentScope.APPLICATION)
public interface RemoteServerListener extends EventListener {
  void serverAdded(@Nonnull RemoteServer<?> server);

  void serverRemoved(@Nonnull RemoteServer<?> server);
}
