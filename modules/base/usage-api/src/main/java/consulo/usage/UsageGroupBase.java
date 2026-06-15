package consulo.usage;

import consulo.annotation.access.RequiredReadAction;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.virtualFileSystem.status.FileStatus;
import org.jspecify.annotations.Nullable;

/**
 * @author nik
 */
public abstract class UsageGroupBase implements UsageGroup {
    @Override
    public void update() {
    }

    @Override
    public @Nullable FileStatus getFileStatus() {
        return null;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public Image getIcon() {
        return null;
    }

    @Override
    @RequiredUIAccess
    public void navigate(boolean focus) {
    }

    @Override
    @RequiredReadAction
    public boolean canNavigate() {
        return false;
    }

    @Override
    @RequiredReadAction
    public boolean canNavigateToSource() {
        return false;
    }
}
