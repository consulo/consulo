// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.remoteServer.runtime.ui;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.project.Project;
import consulo.remoteServer.runtime.ServerConnection;
import consulo.util.lang.function.Condition;
import org.jetbrains.annotations.NotNull;

@ServiceAPI(ComponentScope.PROJECT)
public abstract class RemoteServersView {
    public static RemoteServersView getInstance(@NotNull Project project) {
        return project.getInstance(RemoteServersView.class);
    }

    public abstract void showServerConnection(@NotNull ServerConnection<?> connection);

    public abstract void showDeployment(@NotNull ServerConnection<?> connection, @NotNull String deploymentName);

    public abstract void registerTreeNodeSelector(@NotNull ServersTreeNodeSelector selector,
                                                  @NotNull Condition<ServerConnection<?>> condition);
}
