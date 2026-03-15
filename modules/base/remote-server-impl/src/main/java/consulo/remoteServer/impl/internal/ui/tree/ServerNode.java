package consulo.remoteServer.impl.internal.ui.tree;

import consulo.project.Project;
import org.jspecify.annotations.Nullable;

public interface ServerNode extends ServersTreeNode {
    @Nullable
    Project getProject();
}
