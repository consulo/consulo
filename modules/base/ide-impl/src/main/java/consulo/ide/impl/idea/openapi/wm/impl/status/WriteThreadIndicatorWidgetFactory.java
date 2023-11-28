// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.openapi.wm.impl.status;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.ApplicationManager;
import consulo.application.ApplicationProperties;
import consulo.application.concurrent.DataLock;
import consulo.application.impl.internal.BaseApplication;
import consulo.disposer.Disposer;
import consulo.project.Project;
import consulo.project.ui.wm.CustomStatusBarWidget;
import consulo.project.ui.wm.StatusBar;
import consulo.project.ui.wm.StatusBarWidget;
import consulo.project.ui.wm.StatusBarWidgetFactory;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.UIBundle;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.ThreeState;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.Nls;

import javax.swing.*;
import java.awt.*;
import java.util.Deque;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicIntegerArray;

@ExtensionImpl(id = "writeActionWidget", order = "after fatalErrorWidget")
public class WriteThreadIndicatorWidgetFactory implements StatusBarWidgetFactory {
  private static final String ID = "WriteThread";

  @Override
  public
  @Nonnull
  String getId() {
    return ID;
  }

  @Override
  @Nls
  @Nonnull
  public String getDisplayName() {
    return UIBundle.message("status.bar.write.thread.widget.name");
  }

  @Override
  public boolean isAvailable(@Nonnull Project project) {
    return ApplicationProperties.isInSandbox();
  }

  @Override
  public
  @Nonnull
  StatusBarWidget createWidget(@Nonnull Project project) {
    return new WriteThreadWidget();
  }

  @Override
  public void disposeWidget(@Nonnull StatusBarWidget widget) {
    Disposer.dispose(widget);
  }

  @Override
  public boolean isConfigurable() {
    return ApplicationProperties.isInSandbox();
  }

  @Override
  public boolean canBeEnabledOn(@Nonnull StatusBar statusBar) {
    return ApplicationProperties.isInSandbox();
  }

  @Override
  public boolean isEnabledByDefault() {
    return false;
  }

  private static class WriteThreadWidget implements CustomStatusBarWidget {
    private static final Dimension WIDGET_SIZE = new Dimension(100, 20);
    private final JPanel myComponent = new MyComponent();
    private final Deque<AtomicIntegerArray> myStatsDeque = new LinkedBlockingDeque<>();
    private volatile AtomicIntegerArray myCurrentStats = new AtomicIntegerArray(4);

    private final Timer myTimer = new Timer(500, e -> {
      myStatsDeque.add(myCurrentStats);
      while (myStatsDeque.size() > WIDGET_SIZE.width) {
        myStatsDeque.pop();
      }
      myCurrentStats = new AtomicIntegerArray(4);
      myComponent.repaint();
    });
    private final java.util.Timer ourTimer2 = new java.util.Timer("Write Thread Widget Timer");


    @Override
    public JComponent getComponent() {
      return myComponent;
    }

    @Nullable
    @Override
    public WidgetPresentation getPresentation() {
      return null;
    }

    @Nonnull
    @Override
    public String ID() {
      return ID;
    }

    @Override
    public void install(@Nonnull StatusBar statusBar) {
      BaseApplication application = ObjectUtil.tryCast(ApplicationManager.getApplication(), BaseApplication.class);
      if (application == null) {
        return;
      }

      ourTimer2.scheduleAtFixedRate(new TimerTask() {
        @Override
        public void run() {
          boolean currentValue = DataLock.getInstance().isWriteLockedByAnyThread();
          AtomicIntegerArray currentStats = myCurrentStats;
          currentStats.incrementAndGet((currentValue ? ThreeState.YES : ThreeState.NO).ordinal());
          currentStats.incrementAndGet(3);
        }
      }, 0, 1);
      myTimer.start();
    }

    @Override
    public void dispose() {
      ourTimer2.cancel();
      myTimer.stop();
    }

    private class MyComponent extends JPanel {
      @Override
      public Dimension getPreferredSize() {
        return WIDGET_SIZE;
      }

      @Override
      public Dimension getMinimumSize() {
        return WIDGET_SIZE;
      }

      @Override
      public Dimension getMaximumSize() {
        return WIDGET_SIZE;
      }

      @Override
      public void paint(Graphics g) {
        super.paint(g);
        if (g instanceof Graphics2D) {
          Graphics2D g2d = (Graphics2D)g;

          int offsetx = 0;
          for (AtomicIntegerArray stats : myStatsDeque) {
            g2d.setColor(JBColor.GRAY);
            g2d.fillRect(offsetx, 0, 1, WIDGET_SIZE.height);

            int sum = stats.get(3);
            int offsety = 0;
            int height;

            if (sum > 0) {
              g2d.setColor(JBColor.RED);
              height = (stats.get(0) * WIDGET_SIZE.height + sum - 1) / sum;
              g2d.fillRect(offsetx, WIDGET_SIZE.height - offsety - height, 1, height);
              offsety -= height;

              g2d.setColor(JBColor.GREEN);
              height = (stats.get(1) * WIDGET_SIZE.height + sum - 1) / sum;
              g2d.fillRect(offsetx, WIDGET_SIZE.height - offsety - height, 1, height);
            }

            offsetx++;
          }
        }
      }
    }
  }
}
