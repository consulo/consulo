package consulo.remoteServer.impl.internal.ui.tree;

import consulo.project.Project;
import jakarta.annotation.Nullable;

public interface ServerNode extends ServersTreeNode {
    @Nullable
    Project getProject();
}
