// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.desktop.awt.os.mac.internal.touchBar;

import consulo.application.ApplicationManager;
import consulo.ui.ex.action.AnAction;
import jakarta.annotation.Nonnull;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

final class TouchBarStats {
    private static final Map<String, TouchBarStats> ourStats = new HashMap<>();

    private final String name;
    private final Map<String, AnActionStats> actionStats = new ConcurrentHashMap<>();
    private final AtomicLong[] myCounters = new AtomicLong[StatsCounters.values().length];

    private TouchBarStats(String name) {
        this.name = name;
        Arrays.setAll(myCounters, i -> new AtomicLong(0));
    }

    static @Nonnull TouchBarStats getStats(@Nonnull String touchbarName) {
        return ourStats.computeIfAbsent(touchbarName, s -> new TouchBarStats(touchbarName));
    }

    static void printAll(@Nonnull PrintStream out) {
        for (TouchBarStats tbs : ourStats.values()) {
            tbs.print(out);
        }
    }

    void print(@Nonnull PrintStream out) {
        out.printf("========================= %s =========================", name);
        out.println();
        for (StatsCounters sc : StatsCounters.values()) {
            String name = sc.name();
            long val = myCounters[sc.ordinal()].get();
            if (val == 0) // skip non-informative counters
                continue;
            if (name.endsWith("DurationNs")) {
                if (val < 1000) // skip non-informative counters
                    continue;
                name = name.replace("DurationNs", "DurationMs");
                val /= 1000000L;
            }
            out.printf("%s=%d\n", name, val);
        }
        if (!actionStats.isEmpty()) {
            AnActionStats total = new AnActionStats("total");
            for (AnActionStats as : actionStats.values()) {
                total.accumulate(as);
            }
            total.print(out);
        }
    }

    void incrementCounter(@Nonnull StatsCounters cnt) {
        myCounters[cnt.ordinal()].incrementAndGet();
    }

    void incrementCounter(@Nonnull StatsCounters cnt, long value) {
        myCounters[cnt.ordinal()].addAndGet(value);
    }

    @Nonnull
    AnActionStats getActionStats(@Nonnull String actionId) {
        return actionStats.computeIfAbsent(actionId, s -> new AnActionStats(s));
    }

    @Nonnull
    AnActionStats getActionStats(@Nonnull AnAction action) {
        final String actId = Helpers.getActionId(action);
        return actionStats.computeIfAbsent(actId, s -> new AnActionStats(s));
    }

    static final class AnActionStats {
        final @Nonnull String actionId;

        long totalUpdateDurationNs;
        long maxUpdateDurationNs;
        boolean isBackgroundThread = false;

        long updateViewNs;

        // icon stats
        int iconUpdateIconRasterCount;

        long iconUpdateNativePeerDurationNs; // time spent in _updateNativePeer
        long iconGetDarkDurationNs;   // time spent in IconLoader.getDarkIcon
        long iconRenderingDurationNs; // time spent in NST._getRaster
        long iconLoadingDurationNs;   // time spent in IconLoader.getIcon

        AnActionStats(@Nonnull String actionId) {
            this.actionId = actionId;
        }

        void onUpdate(long updateDurationNs) {
            isBackgroundThread |= !ApplicationManager.getApplication().isDispatchThread();
            totalUpdateDurationNs += updateDurationNs;
            maxUpdateDurationNs = Math.max(maxUpdateDurationNs, updateDurationNs);
        }

        void accumulate(AnActionStats other) {
            this.totalUpdateDurationNs += other.totalUpdateDurationNs;
            this.maxUpdateDurationNs = Math.max(maxUpdateDurationNs, other.maxUpdateDurationNs);

            this.updateViewNs += other.updateViewNs;

            // icon stats
            this.iconUpdateIconRasterCount += other.iconUpdateIconRasterCount;

            this.iconUpdateNativePeerDurationNs += other.iconUpdateNativePeerDurationNs; // time spent in _updateNativePeer
            this.iconGetDarkDurationNs += other.iconGetDarkDurationNs; // time spent in IconLoader.getDarkIcon
            this.iconRenderingDurationNs += other.iconRenderingDurationNs; // time spent in NST._getRaster
            this.iconLoadingDurationNs += other.iconLoadingDurationNs; // time spent in IconLoader.getIcon
        }

        void print(@Nonnull PrintStream out) {
            out.printf("act '%s':\n", actionId);

            printSignificantValue(out, "iconUpdateIconRasterCount", iconUpdateIconRasterCount);
            printSignificantValue(out, "totalUpdateDurationNs", totalUpdateDurationNs);
            printSignificantValue(out, "updateViewNs", updateViewNs);
            printSignificantValue(out, "iconUpdateNativePeerDurationNs", iconUpdateNativePeerDurationNs);
            printSignificantValue(out, "iconGetDarkDurationNs", iconGetDarkDurationNs);
            printSignificantValue(out, "iconRenderingDurationNs", iconRenderingDurationNs);
        }

        private static void printSignificantValue(@Nonnull PrintStream out, @Nonnull String name, long val) {
            if (val == 0) // skip non-informative counters
                return;
            if (name.endsWith("DurationNs") || name.endsWith("Ns")) {
                if (val < 1000) // skip non-informative counters
                    return;
                name = name.replace("Ns", "Ms");
                val /= 1000000L;
            }
            out.printf("\t%s=%d\n", name, val);
        }
    }
}