// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.openapi.wm.impl.status;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.application.impl.internal.BaseApplication;
import consulo.project.Project;
import consulo.project.ui.wm.CustomStatusBarWidget;
import consulo.project.ui.wm.StatusBar;
import consulo.project.ui.wm.StatusBarWidget;
import consulo.project.ui.wm.StatusBarWidgetFactory;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.localize.UILocalize;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.ThreeState;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Deque;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicIntegerArray;

@ExtensionImpl(id = "writeActionWidget", order = "before memoryIndicatorWidget, last")
public class WriteThreadIndicatorWidgetFactory implements StatusBarWidgetFactory {
    @Override
    @Nonnull
    public String getDisplayName() {
        return UILocalize.statusBarWriteThreadWidgetName().get();
    }

    @Override
    public boolean isAvailable(@Nonnull Project project) {
        return project.getApplication().isInternal();
    }

    @Override
    public
    @Nonnull
    StatusBarWidget createWidget(@Nonnull Project project) {
        return new WriteThreadWidget(this);
    }

    @Override
    public boolean isConfigurable() {
        return Application.get().isInternal();
    }

    @Override
    public boolean canBeEnabledOn(@Nonnull StatusBar statusBar) {
        return Application.get().isInternal();
    }

    @Override
    public boolean isEnabledByDefault() {
        return false;
    }

    private static class WriteThreadWidget implements CustomStatusBarWidget {
        private static final Dimension WIDGET_SIZE = new Dimension(100, 20);
        private final JPanel myComponent = new MyComponent();
        private final Deque<AtomicIntegerArray> myStatsDeque = new LinkedBlockingDeque<>();
        private final StatusBarWidgetFactory myFactory;
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

        public WriteThreadWidget(StatusBarWidgetFactory factory) {
            myFactory = factory;
        }

        @Nonnull
        @Override
        public String getId() {
            return myFactory.getId();
        }

        @Nonnull
        @Override
        public JComponent getComponent() {
            return myComponent;
        }

        @Nullable
        @Override
        public WidgetPresentation getPresentation() {
            return null;
        }


        @Override
        public void install(@Nonnull StatusBar statusBar) {
            BaseApplication application = ObjectUtil.tryCast(Application.get(), BaseApplication.class);
            if (application == null) {
                return;
            }

            ourTimer2.scheduleAtFixedRate(
                new TimerTask() {
                    @Override
                    public void run() {
                        boolean currentValue = application.isCurrentWriteOnUIThread();
                        AtomicIntegerArray currentStats = myCurrentStats;
                        currentStats.incrementAndGet((currentValue ? ThreeState.YES : ThreeState.NO).ordinal());
                        currentStats.incrementAndGet(3);
                    }
                },
                0,
                1
            );
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
                    Graphics2D g2d = (Graphics2D) g;

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
