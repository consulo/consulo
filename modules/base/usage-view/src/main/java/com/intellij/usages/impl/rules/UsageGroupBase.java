package com.intellij.usages.impl.rules;

import com.intellij.openapi.util.AsyncResult;
import com.intellij.openapi.vcs.FileStatus;
import com.intellij.usages.UsageGroup;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import javax.swing.*;

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
  public Icon getIcon(boolean isOpen) {
    return null;
  }

  @Nonnull
  @Override
  public AsyncResult<Void> navigateAsync(boolean requestFocus) {
    return AsyncResult.resolved();
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
