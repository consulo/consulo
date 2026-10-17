// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.project.ui.impl.internal.wm.statusBar;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.application.internal.ApplicationEx;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.project.ui.wm.StatusBar;
import consulo.project.ui.wm.StatusBarWidget;
import consulo.project.ui.wm.StatusBarWidgetFactory;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.localize.UILocalize;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import consulo.ui.image.canvas.Canvas2D;
import consulo.ui.style.StandardColors;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.ThreeState;
import org.jspecify.annotations.Nullable;

import java.util.Deque;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicIntegerArray;

@ExtensionImpl(id = "writeActionWidget", order = "before memoryIndicatorWidget, last")
public class WriteThreadIndicatorWidgetFactory implements StatusBarWidgetFactory {
    @Override
    
    public String getDisplayName() {
        return UILocalize.statusBarWriteThreadWidgetName().get();
    }

    @Override
    public boolean isAvailable(Project project) {
        return project.getApplication().isInternal();
    }

    @Override
    public
    
    StatusBarWidget createWidget(Project project) {
        return new WriteThreadWidget(this);
    }

    @Override
    public boolean isConfigurable() {
        return Application.get().isInternal();
    }

    @Override
    public boolean canBeEnabledOn(StatusBar statusBar) {
        return Application.get().isInternal();
    }

    @Override
    public boolean isEnabledByDefault() {
        return false;
    }

    private static class WriteThreadWidget implements StatusBarWidget, StatusBarWidget.IconPresentation {
        private static final int WIDTH = 100;
        private static final int HEIGHT = 20;

        private final Deque<AtomicIntegerArray> myStatsDeque = new LinkedBlockingDeque<>();
        private final StatusBarWidgetFactory myFactory;
        private final Timer mySampler = new Timer("Write Thread Widget Timer");
        private volatile AtomicIntegerArray myCurrentStats = new AtomicIntegerArray(4);

        private @Nullable StatusBar myStatusBar;
        private @Nullable ScheduledFuture<?> myTick;
        private @Nullable Image myImage;

        public WriteThreadWidget(StatusBarWidgetFactory factory) {
            myFactory = factory;
        }

        @Override
        public String getId() {
            return myFactory.getId();
        }

        @Override
        public WidgetPresentation getPresentation() {
            return this;
        }

        @Override
        public @Nullable Image getIcon() {
            return myImage;
        }

        @Override
        public LocalizeValue getTooltipText() {
            return LocalizeValue.empty();
        }

        @Override
        @RequiredUIAccess
        public void install(StatusBar statusBar) {
            ApplicationEx application = ObjectUtil.tryCast(Application.get(), ApplicationEx.class);
            if (application == null) {
                return;
            }

            myStatusBar = statusBar;

            mySampler.scheduleAtFixedRate(
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
            myTick = UIAccess.current().getScheduler().scheduleWithFixedDelay(this::tick, 500, 500, TimeUnit.MILLISECONDS);
        }

        @RequiredUIAccess
        private void tick() {
            myStatsDeque.add(myCurrentStats);
            while (myStatsDeque.size() > WIDTH) {
                myStatsDeque.pop();
            }
            myCurrentStats = new AtomicIntegerArray(4);

            myImage = ImageEffects.canvas(WIDTH, HEIGHT, this::paint);

            StatusBar statusBar = myStatusBar;
            if (statusBar != null) {
                statusBar.updateWidget(getId());
            }
        }

        private void paint(Canvas2D canvas) {
            int offsetx = 0;
            for (AtomicIntegerArray stats : myStatsDeque) {
                canvas.setFillStyle(StandardColors.GRAY);
                canvas.fillRect(offsetx, 0, 1, HEIGHT);

                int sum = stats.get(3);
                int offsety = 0;
                int height;

                if (sum > 0) {
                    canvas.setFillStyle(StandardColors.RED);
                    height = (stats.get(0) * HEIGHT + sum - 1) / sum;
                    canvas.fillRect(offsetx, HEIGHT - offsety - height, 1, height);
                    offsety -= height;

                    canvas.setFillStyle(StandardColors.GREEN);
                    height = (stats.get(1) * HEIGHT + sum - 1) / sum;
                    canvas.fillRect(offsetx, HEIGHT - offsety - height, 1, height);
                }

                offsetx++;
            }
        }

        @Override
        public void dispose() {
            myStatusBar = null;
            mySampler.cancel();

            ScheduledFuture<?> tick = myTick;
            if (tick != null) {
                tick.cancel(false);
                myTick = null;
            }
        }
    }
}
