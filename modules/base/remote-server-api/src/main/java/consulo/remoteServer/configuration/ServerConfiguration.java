package consulo.remoteServer.configuration;

import consulo.component.persist.PersistentStateComponent;
import org.jspecify.annotations.Nullable;

public abstract class ServerConfiguration {
    public abstract PersistentStateComponent<?> getSerializer();

    public @Nullable String getCustomToolWindowId() {
        return null;
    }
}
