package consulo.builtinWebServer.impl.jsonRpc;

import org.jspecify.annotations.Nullable;

import java.util.EventListener;
import java.util.List;
import java.util.Map;

public interface ClientListener extends EventListener {
    void connected(Client client, @Nullable Map<String, List<String>> parameters);

    void disconnected(Client client);
}
