package consulo.remoteServer.configuration;

import consulo.component.persist.PersistentStateComponent;
import org.jetbrains.annotations.Nullable;

public abstract class ServerConfiguration {
    public abstract PersistentStateComponent<?> getSerializer();

    @Nullable
    public String getCustomToolWindowId() {
        return null;
    }
}
