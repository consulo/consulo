// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.application.internal.perfomance;

import org.jspecify.annotations.Nullable;

import java.util.concurrent.TimeUnit;

final class ActivityImpl implements Activity {
    private final @Nullable String myName;
    private @Nullable String myDescription = null;

    private final String myThreadName;
    private final long myThreadId;

    private final long myStart;
    private long myEnd;

    // null doesn't mean root - not obligated to set parent, only as hint
    private final @Nullable ActivityImpl myParent;

    private @Nullable ActivityCategory category;

    private final @Nullable String myPluginId;

    ActivityImpl(@Nullable String name, @Nullable String pluginId) {
        this(name, StartUpMeasurer.getCurrentTime(), null, pluginId);
    }

    ActivityImpl(@Nullable String name, long start, @Nullable ActivityImpl parent, @Nullable String pluginId) {
        myName = name;
        myStart = start;
        myParent = parent;
        myPluginId = pluginId;

        Thread thread = Thread.currentThread();
        myThreadId = thread.getId();
        myThreadName = thread.getName();
    }

    public String getThreadName() {
        return myThreadName;
    }

    public long getThreadId() {
        return myThreadId;
    }

    public @Nullable ActivityImpl getParent() {
        return myParent;
    }

    public @Nullable ActivityCategory getCategory() {
        return category;
    }

    void setCategory(@Nullable ActivityCategory value) {
        category = value;
    }

    // and how do we can sort correctly, when parent item equals to child (start and end),
    // also there is another child with start equals to end?
    // so, parent added to API but as it was not enough, decided to measure time in nanoseconds instead of ms to mitigate such situations
    @Override
    public ActivityImpl startChild(String name) {
        ActivityImpl activity = new ActivityImpl(name, StartUpMeasurer.getCurrentTime(), this, myPluginId);
        activity.category = category;
        return activity;
    }

    public @Nullable String getName() {
        return myName;
    }

    public @Nullable String getDescription() {
        return myDescription;
    }

    public @Nullable String getPluginId() {
        return myPluginId;
    }

    public long getStart() {
        return myStart;
    }

    public long getEnd() {
        return myEnd;
    }

    void setEnd(long end) {
        assert this.myEnd == 0;
        this.myEnd = end;
    }

    @Override
    public void end() {
        assert myEnd == 0 : "not started or already ended";
        myEnd = StartUpMeasurer.getCurrentTime();
        StartUpMeasurer.addActivity(this);
    }

    @Override
    public void setDescription(String value) {
        myDescription = value;
    }

    @Override
    public Activity endAndStart(String name) {
        end();
        ActivityImpl activity = new ActivityImpl(name, /* start = */myEnd, myParent, /* level = */ myPluginId);
        activity.setCategory(category);
        return activity;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ActivityImpl(name=").append(myName).append(", start=");
        nanoToString(myStart, builder);
        builder.append(", end=");
        nanoToString(myEnd, builder);
        builder.append(", category=").append(category).append(")");
        return builder.toString();
    }

    private static void nanoToString(long start, StringBuilder builder) {
        builder.append(TimeUnit.NANOSECONDS.toMillis(start - StartUpMeasurer.getStartTime()))
            .append("ms (").append(TimeUnit.NANOSECONDS.toMicros(start - StartUpMeasurer.getStartTime())).append("μs)");
    }
}