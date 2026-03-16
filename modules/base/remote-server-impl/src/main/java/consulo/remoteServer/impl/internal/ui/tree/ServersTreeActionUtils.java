// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.remoteServer.impl.internal.ui.tree;

import consulo.ui.ex.action.AnActionEvent;
import org.jspecify.annotations.Nullable;

import static consulo.execution.service.ServiceViewActionUtils.getTarget;

public final class ServersTreeActionUtils {
    private ServersTreeActionUtils() {
    }

    public static ServersTreeStructure.@Nullable RemoteServerNode getRemoteServerTarget(AnActionEvent e) {
        return getTarget(e, ServersTreeStructure.RemoteServerNode.class);
    }
}
