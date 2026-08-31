// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ui.ex.popup;

import org.jspecify.annotations.Nullable;

import java.util.concurrent.Callable;

/**
 * Use this "mock" popup step when you need long processing (I/O, network activity etc.) to build real one.
 * The real example is a list of processes on remote host you can connect to with debugger.
 * This step would be started as 'callable' on pooled thread as soon as AsyncPopupImpl instance with "Loading..." text is being created
 * When real popup step is obtained from the background task, mock item would be automatically replaced with it.
 */
public abstract class AsyncPopupStep<T> implements PopupStep<T>, Callable<PopupStep> {

  /**
   * Called when the async popup is disposed to cancel any background work.
   * Subclasses should override to cancel progress indicators or other resources.
   */
  public void cancelBackgroundWork() {
  }

  @Override
  public @Nullable String getTitle() {
    return null;
  }

  @Override
  public @Nullable PopupStep<T> onChosen(T selectedValue, boolean finalChoice) {
    return null;
  }

  @Override
  public boolean hasSubstep(T selectedValue) {
    return false;
  }

  @Override
  public void canceled() {

  }

  @Override
  public boolean isMnemonicsNavigationEnabled() {
    return false;
  }

  @Override
  public @Nullable MnemonicNavigationFilter<T> getMnemonicNavigationFilter() {
    return null;
  }

  @Override
  public boolean isSpeedSearchEnabled() {
    return false;
  }

  @Override
  public @Nullable SpeedSearchFilter<T> getSpeedSearchFilter() {
    return null;
  }

  @Override
  public boolean isAutoSelectionEnabled() {
    return false;
  }

  @Override
  public @Nullable Runnable getFinalRunnable() {
    return null;
  }
}
