// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.build.ui.impl.internal.event;

import consulo.annotation.DeprecationInfo;
import consulo.build.ui.event.BuildEvent;
import consulo.build.ui.event.BuildEventsNls;
import consulo.localize.LocalizeValue;
import org.jspecify.annotations.Nullable;

/**
 * @author Vladislav.Soroka
 */
public abstract class AbstractBuildEvent implements BuildEvent {
    private final Object myEventId;
    private @Nullable Object myParentId;
    private final long myEventTime;
    private final LocalizeValue myMessage;
    private LocalizeValue myHint = LocalizeValue.empty();
    private LocalizeValue myDescription = LocalizeValue.empty();

    public AbstractBuildEvent(Object eventId, @Nullable Object parentId, long eventTime, LocalizeValue message) {
        myEventId = eventId;
        myParentId = parentId;
        myEventTime = eventTime;
        myMessage = message;
    }

    @Override
    public Object getId() {
        return myEventId;
    }

    @Override
    public @Nullable Object getParentId() {
        return myParentId;
    }

    public void setParentId(@Nullable Object parentId) {
        myParentId = parentId;
    }

    @Override
    public long getEventTime() {
        return myEventTime;
    }

    @Override
    public LocalizeValue getMessage() {
        return myMessage;
    }

    @Override
    public LocalizeValue getHint() {
        return myHint;
    }

    public void setHint(LocalizeValue hint) {
        myHint = hint;
    }

    @Deprecated
    @DeprecationInfo("Use variant with LocalizeValue")
    public void setHint(@Nullable @BuildEventsNls.Hint String hint) {
        setHint(LocalizeValue.ofNullable(hint));
    }

    @Override
    public LocalizeValue getDescription() {
        return myDescription;
    }

    public void setDescription(LocalizeValue description) {
        myDescription = description;
    }

    @Deprecated
    @DeprecationInfo("Use variant with LocalizeValue")
    public void setDescription(@Nullable @BuildEventsNls.Description String description) {
        setDescription(LocalizeValue.ofNullable(description));
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "myEventId=" + myEventId +
            ", myParentId=" + myParentId +
            ", myEventTime=" + myEventTime +
            ", myMessage='" + myMessage + '\'' +
            ", myHint='" + myHint + '\'' +
            ", myDescription='" + myDescription + '\'' +
            '}';
    }
}
