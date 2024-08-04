// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.application.internal.perfomance;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.concurrent.TimeUnit;

final class ActivityImpl implements Activity {
    private final String name;
    private String description;

    private final String threadName;
    private final long threadId;

    private final long start;
    private long end;

    // null doesn't mean root - not obligated to set parent, only as hint
    private final ActivityImpl parent;

    @Nullable
    private ActivityCategory category;

    @Nullable
    private final String pluginId;

    ActivityImpl(@Nullable String name, @Nullable String pluginId) {
        this(name, StartUpMeasurer.getCurrentTime(), null, pluginId);
    }

    ActivityImpl(@Nullable String name, long start, @Nullable ActivityImpl parent, @Nullable String pluginId) {
        this.name = name;
        this.start = start;
        this.parent = parent;
        this.pluginId = pluginId;

        Thread thread = Thread.currentThread();
        threadId = thread.getId();
        threadName = thread.getName();
    }

    @Nonnull
    public String getThreadName() {
        return threadName;
    }

    public long getThreadId() {
        return threadId;
    }

    @Nullable
    public ActivityImpl getParent() {
        return parent;
    }

    @Nullable
    public ActivityCategory getCategory() {
        return category;
    }

    void setCategory(@Nullable ActivityCategory value) {
        category = value;
    }

    // and how do we can sort correctly, when parent item equals to child (start and end), also there is another child with start equals to end?
    // so, parent added to API but as it was not enough, decided to measure time in nanoseconds instead of ms to mitigate such situations
    @Override
    @Nonnull
    public ActivityImpl startChild(@Nonnull String name) {
        ActivityImpl activity = new ActivityImpl(name, StartUpMeasurer.getCurrentTime(), this, pluginId);
        activity.category = category;
        return activity;
    }

    @Nonnull
    public String getName() {
        return name;
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    @Nullable
    public String getPluginId() {
        return pluginId;
    }

    public long getStart() {
        return start;
    }

    public long getEnd() {
        return end;
    }

    void setEnd(long end) {
        assert this.end == 0;
        this.end = end;
    }

    @Override
    public void end() {
        assert end == 0 : "not started or already ended";
        end = StartUpMeasurer.getCurrentTime();
        StartUpMeasurer.addActivity(this);
    }

    @Override
    public void setDescription(@Nonnull String value) {
        description = value;
    }

    @Override
    @Nonnull
    public Activity endAndStart(@Nonnull String name) {
        end();
        ActivityImpl activity = new ActivityImpl(name, /* start = */end, parent, /* level = */ pluginId);
        activity.setCategory(category);
        return activity;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ActivityImpl(name=").append(name).append(", start=");
        nanoToString(start, builder);
        builder.append(", end=");
        nanoToString(end, builder);
        builder.append(", category=").append(category).append(")");
        return builder.toString();
    }

    private static void nanoToString(long start, @Nonnull StringBuilder builder) {
        builder.append(TimeUnit.NANOSECONDS.toMillis(start - StartUpMeasurer.getStartTime())).append("ms (").append(TimeUnit.NANOSECONDS.toMicros(start - StartUpMeasurer.getStartTime())).append("μs)");
    }
}