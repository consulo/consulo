// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ui.components;

import consulo.ui.ex.awt.LoadingDecorator;
import consulo.ui.ex.awt.NonOpaquePanel;
import consulo.ui.ex.awt.AsyncProcessIcon;
import consulo.disposer.Disposable;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class ProgressBarLoadingDecorator extends LoadingDecorator {
  private final AtomicBoolean loadingStarted = new AtomicBoolean(false);
  private JProgressBar myProgressBar;

  public ProgressBarLoadingDecorator(@Nonnull JPanel contentPanel, @Nonnull Disposable disposable, int startDelayMs) {
    super(contentPanel, disposable, startDelayMs, true);
  }

  @Override
  protected NonOpaquePanel customizeLoadingLayer(JPanel parent, JLabel text, AsyncProcessIcon icon) {
    parent.setLayout(new BorderLayout());
    NonOpaquePanel result = new NonOpaquePanel();
    result.setLayout(new BoxLayout(result, BoxLayout.Y_AXIS));
    myProgressBar = new JProgressBar();
    myProgressBar.setIndeterminate(true);
    myProgressBar.putClientProperty("ProgressBar.stripeWidth", 2);
    myProgressBar.putClientProperty("ProgressBar.flatEnds", Boolean.TRUE);
    result.add(myProgressBar);
    parent.add(result, BorderLayout.NORTH);
    return result;
  }

  @Nonnull
  public JProgressBar getProgressBar() {
    return myProgressBar;
  }

  public void startLoading() {
    if (loadingStarted.compareAndSet(false, true)) {
      super.startLoading(false);
    }
  }

  @Override
  public void stopLoading() {
    if (loadingStarted.compareAndSet(true, false)) {
      super.stopLoading();
    }
  }
}
