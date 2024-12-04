// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.remoteServer.impl.internal.ui.tree;

import consulo.remoteServer.runtime.ServerConnection;
import jakarta.annotation.Nonnull;

public interface ServerTreeNodeExpander {
    void expand(@Nonnull ServerConnection<?> connection, @Nonnull String deploymentName);
}
