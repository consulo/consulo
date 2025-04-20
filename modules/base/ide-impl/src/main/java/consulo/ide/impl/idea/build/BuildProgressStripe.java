// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.build;

import consulo.disposer.Disposable;
import consulo.ide.impl.idea.ui.components.ProgressBarLoadingDecorator;
import consulo.ui.ex.awt.JBPanel;
import consulo.util.lang.lazy.LazyValue;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;

class BuildProgressStripe extends JBPanel {
  @Nonnull
  private final JBPanel myPanel;
  private final LazyValue<ProgressBarLoadingDecorator> myCreateLoadingDecorator;
  private ProgressBarLoadingDecorator myDecorator;

  BuildProgressStripe(@Nonnull JComponent targetComponent, @Nonnull Disposable parent, int startDelayMs) {
    super(new BorderLayout());
    myPanel = new JBPanel(new BorderLayout());
    myPanel.setOpaque(false);
    myPanel.add(targetComponent);
    myCreateLoadingDecorator = LazyValue.notNull(() -> new ProgressBarLoadingDecorator(myPanel, parent, startDelayMs));
    createLoadingDecorator();
  }

  public void updateProgress(long total, long progress) {
    if (total == progress) {
      stopLoading();
      return;
    }
    boolean isDeterminate = total > 0 && progress > 0;
    JProgressBar progressBar = getProgressBar();
    boolean isProgressBarIndeterminate = progressBar.isIndeterminate();
    if (isDeterminate) {
      startLoading();
      progressBar.setValue(Math.toIntExact(progress * 100 / total));
      if (isProgressBarIndeterminate) {
        progressBar.setIndeterminate(false);
      }
    }
    else if (!isProgressBarIndeterminate) {
      progressBar.setIndeterminate(true);
    }
  }

  void startLoading() {
    myDecorator.startLoading();
  }

  void stopLoading() {
    JProgressBar progressBar = getProgressBar();
    if (!progressBar.isIndeterminate()) {
      progressBar.setValue(100);
    }
    myDecorator.stopLoading();
  }

  private JProgressBar getProgressBar() {
    return myCreateLoadingDecorator.get().getProgressBar();
  }

  private void createLoadingDecorator() {
    myDecorator = myCreateLoadingDecorator.get();
    add(myDecorator.getComponent(), BorderLayout.CENTER);
    myDecorator.setLoadingText("");
  }
}
