// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.project.ui.impl.internal.wm.statusBar;

import consulo.project.ui.wm.CustomStatusBarWidget;
import consulo.project.ui.wm.StatusBar;
import consulo.project.ui.wm.StatusBarWidgetFactory;
import consulo.ui.Component;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.internal.MemoryIndicator;
import consulo.ui.ex.internal.UIInternalEx;
import consulo.ui.ex.localize.UILocalize;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static consulo.util.io.FileUtil.MEGABYTE;

public final class MemoryUsagePanel implements CustomStatusBarWidget {
    private final StatusBarWidgetFactory myFactory;

    private @Nullable MemoryIndicator myIndicator;
    private @Nullable ScheduledFuture<?> myFuture;
    private long myLastAllocated = -1;
    private long myLastUsed = -1;

    public MemoryUsagePanel(StatusBarWidgetFactory factory) {
        myFactory = factory;
    }

    @Override
    public String getId() {
        return myFactory.getId();
    }

    @Override
    public @Nullable WidgetPresentation getPresentation() {
        return null;
    }

    @Override
    public void install(StatusBar statusBar) {
    }

    @RequiredUIAccess
    @Override
    public Component getUIComponent() {
        MemoryIndicator indicator = myIndicator;
        if (indicator != null) {
            return indicator;
        }

        long maxMemory = Runtime.getRuntime().maxMemory() / MEGABYTE;
        long sample = maxMemory < 1000 ? 999 : maxMemory < 10_000 ? 9_999 : 99_999;

        MemoryIndicator newIndicator = UIInternalEx.get()._Components_memoryIndicator(UILocalize.memoryUsagePanelMessageText(sample, sample));
        newIndicator.addClickListener(event -> {
            //noinspection CallToSystemGC
            System.gc();
            updateState(newIndicator);
        });
        newIndicator.addAttachListener(event -> start(newIndicator));
        newIndicator.addDetachListener(event -> stop());
        updateState(newIndicator);

        myIndicator = newIndicator;
        return newIndicator;
    }

    @Override
    public void dispose() {
        stop();
    }

    @RequiredUIAccess
    private void start(MemoryIndicator indicator) {
        stop();

        myFuture = UIAccess.current().getScheduler().scheduleWithFixedDelay(() -> updateState(indicator), 1, 5, TimeUnit.SECONDS);
    }

    private void stop() {
        ScheduledFuture<?> future = myFuture;
        if (future != null) {
            future.cancel(false);
            myFuture = null;
        }
    }

    @RequiredUIAccess
    private void updateState(MemoryIndicator indicator) {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory() / MEGABYTE;
        long allocatedMemory = runtime.totalMemory() / MEGABYTE;
        long usedMemory = allocatedMemory - runtime.freeMemory() / MEGABYTE;

        if (allocatedMemory == myLastAllocated && usedMemory == myLastUsed) {
            return;
        }

        myLastAllocated = allocatedMemory;
        myLastUsed = usedMemory;

        indicator.setUsage(usedMemory, allocatedMemory, maxMemory, UILocalize.memoryUsagePanelMessageText(usedMemory, maxMemory));
        indicator.setToolTipText(UILocalize.memoryUsagePanelStatisticsMessage(allocatedMemory, usedMemory));
    }
}
