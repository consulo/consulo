// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.remoteServer.runtime.ui;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.project.Project;
import consulo.remoteServer.runtime.ServerConnection;
import consulo.util.lang.function.Condition;
import jakarta.annotation.Nonnull;

@ServiceAPI(ComponentScope.PROJECT)
public abstract class RemoteServersView {
    public static RemoteServersView getInstance(@Nonnull Project project) {
        return project.getInstance(RemoteServersView.class);
    }

    public abstract void showServerConnection(@Nonnull ServerConnection<?> connection);

    public abstract void showDeployment(@Nonnull ServerConnection<?> connection, @Nonnull String deploymentName);

    public abstract void registerTreeNodeSelector(
        @Nonnull ServersTreeNodeSelector selector,
        @Nonnull Condition<ServerConnection<?>> condition
    );
}
