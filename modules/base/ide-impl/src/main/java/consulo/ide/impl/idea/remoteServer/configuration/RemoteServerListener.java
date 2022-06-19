package consulo.ide.impl.idea.remoteServer.configuration;

import consulo.component.messagebus.TopicImpl;
import javax.annotation.Nonnull;

import java.util.EventListener;

/**
 * @author nik
 */
public interface RemoteServerListener extends EventListener {
  TopicImpl<RemoteServerListener> TOPIC = TopicImpl.create("remote servers", RemoteServerListener.class);

  void serverAdded(@Nonnull RemoteServer<?> server);
  void serverRemoved(@Nonnull RemoteServer<?> server);
}
