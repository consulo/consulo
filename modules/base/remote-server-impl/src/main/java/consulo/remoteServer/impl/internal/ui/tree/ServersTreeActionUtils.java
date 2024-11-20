// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.remoteServer.impl.internal.ui.tree;

import consulo.ui.ex.action.AnActionEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static consulo.execution.service.ServiceViewActionUtils.getTarget;

public final class ServersTreeActionUtils {
    private ServersTreeActionUtils() {
    }

    @Nullable
    public static ServersTreeStructure.RemoteServerNode getRemoteServerTarget(@NotNull AnActionEvent e) {
        return getTarget(e, ServersTreeStructure.RemoteServerNode.class);
    }
}
