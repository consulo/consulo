package consulo.ide.impl.idea.remoteServer.agent;

import java.io.File;
import java.util.List;

/**
 * @author michael.golubev
 */
public interface RemoteAgentProxyFactory {

  <T> T createProxy(List<File> libraries, Class<T> agentInterface, String agentClassName) throws Exception;
}
