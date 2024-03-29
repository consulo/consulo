// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.build;

import consulo.application.util.NotNullLazyValue;
import consulo.ui.ex.awt.JBPanel;
import consulo.ide.impl.idea.ui.components.ProgressBarLoadingDecorator;
import consulo.disposer.Disposable;

import jakarta.annotation.Nonnull;
import javax.swing.*;
import java.awt.*;

//@ApiStatus.Internal
class BuildProgressStripe extends JBPanel {
  @Nonnull
  private final JBPanel myPanel;
  private final NotNullLazyValue<ProgressBarLoadingDecorator> myCreateLoadingDecorator;
  private ProgressBarLoadingDecorator myDecorator;

  BuildProgressStripe(@Nonnull JComponent targetComponent, @Nonnull Disposable parent, int startDelayMs) {
    super(new BorderLayout());
    myPanel = new JBPanel(new BorderLayout());
    myPanel.setOpaque(false);
    myPanel.add(targetComponent);
    myCreateLoadingDecorator = NotNullLazyValue.createValue(() -> {
      return new ProgressBarLoadingDecorator(myPanel, parent, startDelayMs);
    });
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
    return myCreateLoadingDecorator.getValue().getProgressBar();
  }

  private void createLoadingDecorator() {
    myDecorator = myCreateLoadingDecorator.getValue();
    add(myDecorator.getComponent(), BorderLayout.CENTER);
    myDecorator.setLoadingText("");
  }
}
