package consulo.remoteServer.impl.internal.ui.tree;

import consulo.project.Project;
import org.jetbrains.annotations.Nullable;

public interface ServerNode extends ServersTreeNode {
    @Nullable
    Project getProject();
}
