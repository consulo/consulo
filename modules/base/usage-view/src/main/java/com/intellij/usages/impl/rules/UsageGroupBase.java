package com.intellij.usages.impl.rules;

import com.intellij.openapi.vcs.FileStatus;
import com.intellij.usages.UsageGroup;
import consulo.ui.image.Image;

import javax.annotation.Nullable;

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
