package consulo.usage;

import consulo.navigation.NavigateOptions;
import consulo.ui.image.Image;
import consulo.virtualFileSystem.status.FileStatus;
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
  public NavigateOptions getNavigateOptions() {
    return NavigateOptions.CANT_NAVIGATE;
  }

  @Override
  public void navigate(boolean focus) {
  }
}
