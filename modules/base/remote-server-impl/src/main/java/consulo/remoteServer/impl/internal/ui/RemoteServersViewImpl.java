package consulo.remoteServer.impl.internal.ui;

import consulo.annotation.component.ServiceImpl;
import consulo.remoteServer.runtime.ServerConnection;
import consulo.remoteServer.runtime.ui.RemoteServersView;
import consulo.remoteServer.runtime.ui.ServersTreeNodeSelector;
import consulo.util.lang.Pair;
import consulo.util.lang.function.Condition;
import jakarta.annotation.Nonnull;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Singleton
@ServiceImpl
public class RemoteServersViewImpl extends RemoteServersView {
    private final List<Pair<ServersTreeNodeSelector, Condition<ServerConnection<?>>>> mySelectors = new CopyOnWriteArrayList<>();

    @Override
    public void showServerConnection(@Nonnull ServerConnection<?> connection) {
        ServersTreeNodeSelector selector = findSelector(connection);
        if (selector != null) {
            selector.select(connection);
        }
    }

    private ServersTreeNodeSelector findSelector(ServerConnection<?> connection) {
        for (Pair<ServersTreeNodeSelector, Condition<ServerConnection<?>>> pair : mySelectors) {
            if (pair.second.value(connection)) {
                return pair.first;
            }
        }
        return null;
    }

    @Override
    public void showDeployment(@Nonnull ServerConnection<?> connection, @Nonnull String deploymentName) {
        ServersTreeNodeSelector selector = findSelector(connection);
        if (selector != null) {
            selector.select(connection, deploymentName);
        }
    }

    @Override
    public void registerTreeNodeSelector(
        @Nonnull ServersTreeNodeSelector selector,
        @Nonnull Condition<ServerConnection<?>> condition
    ) {
        mySelectors.add(Pair.create(selector, condition));
    }
}
