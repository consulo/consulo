package consulo.remoteServer.configuration;

import consulo.component.persist.PersistentStateComponent;
import jakarta.annotation.Nullable;

public abstract class ServerConfiguration {
    public abstract PersistentStateComponent<?> getSerializer();

    @Nullable
    public String getCustomToolWindowId() {
        return null;
    }
}
