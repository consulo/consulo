package consulo.externalService.impl.internal.update;

import consulo.container.plugin.PluginId;
import consulo.container.plugin.PluginDescriptor;

import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2018-07-08
 */
public class PlatformOrPluginNode {
    
    private final PluginId myPluginId;
    private final @Nullable PluginDescriptor myCurrentDescriptor;
    private final @Nullable PluginDescriptor myFutureDescriptor;

    public PlatformOrPluginNode(
        PluginId pluginId,
        @Nullable PluginDescriptor currentDescriptor,
        @Nullable PluginDescriptor futureDescriptor
    ) {
        myPluginId = pluginId;
        myCurrentDescriptor = currentDescriptor;
        myFutureDescriptor = futureDescriptor;

        if (myCurrentDescriptor == null && myFutureDescriptor == null) {
            throw new IllegalArgumentException("Current or future descriptor must be set");
        }
    }

    
    public PluginId getPluginId() {
        return myPluginId;
    }

    public @Nullable PluginDescriptor getCurrentDescriptor() {
        return myCurrentDescriptor;
    }

    public @Nullable PluginDescriptor getFutureDescriptor() {
        return myFutureDescriptor;
    }
}
