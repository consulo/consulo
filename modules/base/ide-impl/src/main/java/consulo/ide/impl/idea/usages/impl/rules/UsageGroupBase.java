package consulo.ide.impl.idea.usages.impl.rules;

import consulo.virtualFileSystem.status.FileStatus;
import consulo.usage.UsageGroup;
import consulo.ui.image.Image;

import jakarta.annotation.Nullable;

/**
 * @author nik
 */
public abstract class UsageGroupBase implements UsageGroup {
  @Override
  public void update() {
  }

  @Nullable
  @Override
  public FileStatus getFileStatus() {
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
  public void navigate(boolean focus) {
  }

  @Override
  public boolean canNavigate() {
    return false;
  }

  @Override
  public boolean canNavigateToSource() {
    return false;
  }
}
