// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.build.ui.impl.internal.event;

import consulo.build.ui.event.BuildEventsNls;
import consulo.build.ui.event.MessageEvent;
import consulo.build.ui.event.MessageEventResult;
import consulo.navigation.Navigatable;
import consulo.project.Project;
import consulo.project.ui.notification.NotificationGroup;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * @author Vladislav.Soroka
 */
public class MessageEventImpl extends AbstractBuildEvent implements MessageEvent {

    
    private final Kind myKind;
    
    private final NotificationGroup myGroup;
    private final @Nullable Navigatable myNavigatable;

    public MessageEventImpl(Object parentId,
                            Kind kind,
                            NotificationGroup group,
                            String message,
                            @Nullable String detailedMessage) {
        this(parentId, kind, group, message, detailedMessage, null);
    }

    public MessageEventImpl(Object parentId,
                            Kind kind,
                            NotificationGroup group,
                            String message,
                            @Nullable String detailedMessage,
                            @Nullable Navigatable navigatable) {
        super(new Object(), parentId, System.currentTimeMillis(), message);
        myKind = kind;
        myGroup = group;
        myNavigatable = navigatable;
        setDescription(detailedMessage);
    }

    @Override
    public final void setDescription(@Nullable @BuildEventsNls.Description String description) {
        super.setDescription(description);
    }

    
    @Override
    public Kind getKind() {
        return myKind;
    }

    
    @Override
    public NotificationGroup getGroup() {
        return myGroup;
    }

    @Nullable
    @Override
    public Navigatable getNavigatable(Project project) {
        return myNavigatable;
    }

    @Override
    public MessageEventResult getResult() {
        return new MessageEventResult() {
            @Override
            public Kind getKind() {
                return myKind;
            }

            @Override
            public String getDetails() {
                return getDescription();
            }
        };
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MessageEventImpl event = (MessageEventImpl) o;
        return Objects.equals(getMessage(), event.getMessage()) && Objects.equals(getDescription(), event.getDescription()) && Objects.equals(myGroup, event.myGroup);
    }

    @Override
    public int hashCode() {
        return Objects.hash(myGroup, getMessage());
    }
}
