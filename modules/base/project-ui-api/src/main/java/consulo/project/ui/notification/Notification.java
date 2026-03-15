/*
 * Copyright 2000-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.project.ui.notification;

import consulo.annotation.DeprecationInfo;
import consulo.application.Application;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ui.internal.NotificationActionInvoker;
import consulo.project.ui.notification.event.NotificationListener;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.popup.Balloon;
import consulo.ui.ex.popup.event.JBPopupAdapter;
import consulo.ui.ex.popup.event.LightweightWindowEvent;
import consulo.ui.image.Image;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.Lists;
import consulo.util.collection.SmartList;
import consulo.util.dataholder.Key;
import consulo.util.lang.function.Functions;
import consulo.util.lang.ref.SoftReference;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * <p>Notification bean class contains <b>title:</b>subtitle, content (plain text or HTML) and actions.</p>
 *
 * <p>The notifications, generally, are shown in the balloons that appear on the screen when the corresponding events take place.</p>
 *
 * <p>Notification balloon is of two types: two or three lines.</p>
 *
 * <p>Two lines: title and content line; title and actions; content line and actions; contents on two lines.</p>
 *
 * <p>Three lines: title and content line and actions; contents on two lines and actions; contents on three lines or more; etc.</p>
 *
 * <p>Warning: be careful not to use the links in HTML content, use {@link #addAction(AnAction)}</p>
 *
 * <p>Construct new notifications starting from {@link NotificationGroup}:</p>
 *
 * <code>GROUP.newError()
 *     .title(...)
 *     .content(...)
 *     .icon(...)
 *     .addAction(...)
 *     .notify(project);
 * </code>
 *
 * @author spleaner
 * @author Alexander Lobas
 * @author UNV
 *
 * @see NotificationService#newError(NotificationGroup)
 * @see NotificationService#newWarn(NotificationGroup)
 * @see NotificationService#newInfo(NotificationGroup)
 * @see NotificationService#newOfType(NotificationGroup, NotificationType)
 */
public class Notification {
    public static class Builder {
        
        final NotificationService myService;
        
        final NotificationGroup myGroup;
        
        final NotificationType myType;

        
        private LocalizeValue myTitle = LocalizeValue.empty();
        
        private LocalizeValue mySubtitle = LocalizeValue.empty();
        
        private LocalizeValue myContent = LocalizeValue.empty();
        @Nullable
        private Image myIcon = null;
        @Nullable
        private Boolean myImportant = null;
        @Nullable
        private NotificationListener myListener = null;
        @Nullable
        private List<Function<Notification, ? extends AnAction>> myActionAdders = null;
        @Nullable
        private Runnable myWhenExpired = null;

        Builder(NotificationService service, NotificationGroup group, NotificationType type) {
            myService = service;
            myGroup = group;
            myType = type;
        }

        public Builder icon(Image icon) {
            if (myIcon != null) {
                throw new IllegalArgumentException("Icon is already initialized");
            }
            myIcon = icon;
            return this;
        }

        public Builder optionalIcon(@Nullable Image icon) {
            return icon != null ? icon(icon) : this;
        }

        public Builder title(LocalizeValue title) {
            if (myTitle.isNotEmpty()) {
                throw new IllegalArgumentException("Title is already initialized");
            }
            myTitle = title;
            return this;
        }

        public Builder subtitle(LocalizeValue subtitle) {
            if (mySubtitle.isNotEmpty()) {
                throw new IllegalArgumentException("Subtitle is already initialized");
            }
            mySubtitle = subtitle;
            return this;
        }

        public Builder content(LocalizeValue content) {
            if (myContent.isNotEmpty()) {
                throw new IllegalArgumentException("Content is already initialized");
            }
            myContent = content;
            return this;
        }

        public Builder important() {
            return important(true);
        }

        public Builder notImportant() {
            return important(false);
        }

        public Builder important(boolean important) {
            if (myImportant != null) {
                throw new IllegalStateException("Flag 'important' is already initialized");
            }
            myImportant = important;
            return this;
        }

        public Builder hyperlinkListener(@RequiredUIAccess NotificationListener listener) {
            if (myListener != null) {
                throw new IllegalArgumentException("Only one hyperlink listener is allowed");
            }
            myListener = listener;
            return this;
        }

        public Builder optionalHyperlinkListener(@Nullable NotificationListener listener) {
            return listener != null ? hyperlinkListener(listener) : this;
        }

        public Builder addAction(AnAction action) {
            return addAction(Functions.constant(action));
        }

        public Builder addAction(LocalizeValue text, @RequiredUIAccess Runnable runnable) {
            return addAction(new DumbAwareAction(text) {
                @Override
                @RequiredUIAccess
                public void actionPerformed(AnActionEvent event) {
                    runnable.run();
                }
            });
        }

        public Builder addAction(LocalizeValue text, @RequiredUIAccess Consumer<AnActionEvent> consumer) {
            return addAction(new DumbAwareAction(text) {
                @Override
                @RequiredUIAccess
                public void actionPerformed(AnActionEvent event) {
                    consumer.accept(event);
                }
            });
        }

        public Builder addClosingAction(LocalizeValue text, @RequiredUIAccess Consumer<AnActionEvent> consumer) {
            return addAction(notification -> new DumbAwareAction(text) {
                @Override
                @RequiredUIAccess
                public void actionPerformed(AnActionEvent event) {
                    notification.expire();
                    consumer.accept(event);
                }
            });
        }

        public Builder addClosingAction(LocalizeValue text, @RequiredUIAccess Runnable runnable) {
            return addAction(notification -> new DumbAwareAction(text) {
                @Override
                @RequiredUIAccess
                public void actionPerformed(AnActionEvent e) {
                    notification.expire();
                    runnable.run();
                }
            });
        }

        public Builder addAction(Function<Notification, ? extends AnAction> actionAdder) {
            if (myActionAdders == null) {
                myActionAdders = new SmartList<>(actionAdder);
            }
            else {
                myActionAdders.add(actionAdder);
            }
            return this;
        }

        public Builder whenExpired(Runnable whenExpired) {
            if (myWhenExpired != null) {
                throw new IllegalStateException("Only one expiration listener is allowed");
            }
            myWhenExpired = whenExpired;
            return this;
        }

        public Notification create() {
            Notification notification = new Notification(myGroup, myIcon, myTitle, mySubtitle, myContent, myType, myListener);
            postInit(notification);
            return notification;
        }

        protected void init(Notification notification) {
            notification.setIcon(myIcon);
            notification.setTitle(myTitle);
            notification.setSubtitle(mySubtitle);
            notification.setContent(myContent);
            postInit(notification);
        }

        private void postInit(Notification notification) {
            if (myImportant != null) {
                notification.setImportant(myImportant);
            }
            if (myListener != null) {
                notification.setListener(myListener);
            }
            if (myActionAdders != null) {
                for (Function<Notification, ? extends AnAction> actionAdder : myActionAdders) {
                    notification.addAction(actionAdder.apply(notification));
                }
            }
            if (myWhenExpired != null) {
                notification.whenExpired(myWhenExpired);
            }
        }

        public void notify(@Nullable Project project) {
            myService.notify(create(), project);
        }

        public Notification notifyAndGet(@Nullable Project project) {
            Notification notification = create();
            myService.notify(notification, project);
            return notification;
        }
    }

    private static final Logger LOG = Logger.getInstance(Notification.class);
    private static final Key<Notification> KEY = Key.create(Notification.class);

    /**
     * Which actions to keep and which to show under the "Actions" dropdown link if actions do not fit horizontally
     * into the width of the notification.
     */
    public enum CollapseActionsDirection {
        KEEP_LEFTMOST,
        KEEP_RIGHTMOST
    }

    
    public final String id;
    
    private final String myGroupId;

    
    private final NotificationType myType;

    @Nullable
    private Image myIcon;
    
    private LocalizeValue myTitle = LocalizeValue.empty();
    
    private LocalizeValue mySubtitle = LocalizeValue.empty();
    
    private LocalizeValue myContent = LocalizeValue.empty();

    private NotificationListener myListener;
    private String myDropDownText;
    private List<AnAction> myActions;
    private CollapseActionsDirection myCollapseActionsDirection = CollapseActionsDirection.KEEP_RIGHTMOST;

    private AtomicReference<Boolean> myExpired = new AtomicReference<>(false);
    private Runnable myWhenExpired;
    private Boolean myImportant;
    private WeakReference<Balloon> myBalloonRef;
    private final long myTimestamp = System.currentTimeMillis();

    /**
     * @param group    notification group
     * @param icon     notification icon, if {@code null} uses icon from type
     * @param title    notification title
     * @param subtitle notification subtitle
     * @param content  notification content
     * @param type     notification type
     * @param listener notification lifecycle listener
     */
    private Notification(
        NotificationGroup group,
        @Nullable Image icon,
        LocalizeValue title,
        LocalizeValue subtitle,
        LocalizeValue content,
        NotificationType type,
        @Nullable NotificationListener listener
    ) {
        myGroupId = group.getId();
        myTitle = title;
        myContent = content;
        myType = type;
        myListener = listener;

        myIcon = icon;
        mySubtitle = subtitle;

        assertHasTitleOrContent();
        id = calculateId();
    }

    /**
     * @param notificationBuilder notification builder
     */
    protected Notification(Builder notificationBuilder) {
        myGroupId = notificationBuilder.myGroup.getId();
        myType = notificationBuilder.myType;
        notificationBuilder.init(this);

        assertHasTitleOrContent();
        id = calculateId();
    }

    /**
     * @param group   notification group
     * @param title   notification title
     * @param content notification content
     * @param type    notification type
     */
    @Deprecated
    @DeprecationInfo("Use NotificationGroup#newError/newWarning/newInfo/newOfType()...create()")
    public Notification(NotificationGroup group, String title, String content, NotificationType type) {
        this(
            NotificationService.getInstance()
                .newOfType(group, type)
                .title(LocalizeValue.of(title))
                .content(LocalizeValue.of(content))
        );
    }

    /**
     * @param group    notification group
     * @param icon     notification icon, if {@code null} uses icon from type
     * @param title    notification title
     * @param subtitle notification subtitle
     * @param content  notification content
     * @param type     notification type
     * @param listener notification lifecycle listener
     */
    @Deprecated
    @DeprecationInfo("Use NotificationGroup#newError/newWarning/newInfo/newOfType()...create()")
    public Notification(
        NotificationGroup group,
        @Nullable Image icon,
        @Nullable String title,
        @Nullable String subtitle,
        @Nullable String content,
        NotificationType type,
        @Nullable NotificationListener listener
    ) {
        this(
            NotificationService.getInstance()
                .newOfType(group, type)
                .optionalIcon(icon)
                .title(LocalizeValue.ofNullable(title))
                .subtitle(LocalizeValue.ofNullable(subtitle))
                .content(LocalizeValue.ofNullable(content))
                .optionalHyperlinkListener(listener)
        );
    }

    /**
     * @param group    notification group
     * @param title    notification title
     * @param content  notification content
     * @param type     notification type
     * @param listener notification lifecycle listener
     */
    @Deprecated
    @DeprecationInfo("Use NotificationGroup#newError/newWarning/newInfo/newOfType()...create()")
    public Notification(
        NotificationGroup group,
        String title,
        String content,
        NotificationType type,
        @Nullable @RequiredUIAccess NotificationListener listener
    ) {
        this(
            NotificationService.getInstance()
                .newOfType(group, type)
                .title(LocalizeValue.of(title))
                .content(LocalizeValue.of(content))
                .optionalHyperlinkListener(listener)
        );
    }

    /**
     * Returns the time (in milliseconds since Jan 1, 1970) when the notification was created.
     */
    public long getTimestamp() {
        return myTimestamp;
    }

    @Nullable
    public Image getIcon() {
        return myIcon;
    }

    
    public Notification setIcon(@Nullable Image icon) {
        myIcon = icon;
        return this;
    }

    
    public String getGroupId() {
        return myGroupId;
    }

    public boolean hasTitle() {
        return myTitle.isNotEmpty() || mySubtitle.isNotEmpty();
    }

    
    public String getTitle() {
        return myTitle.get();
    }

    
    public Notification setTitle(LocalizeValue title) {
        myTitle = title;
        return this;
    }

    @Deprecated
    @DeprecationInfo("Use variant with LocalizeValue")
    
    public Notification setTitle(@Nullable String title) {
        myTitle = LocalizeValue.ofNullable(title);
        return this;
    }

    @Nullable
    public String getSubtitle() {
        return mySubtitle.get();
    }

    
    public Notification setSubtitle(LocalizeValue subtitle) {
        mySubtitle = subtitle;
        return this;
    }

    @Deprecated
    @DeprecationInfo("Use variant with LocalizeValue")
    
    public Notification setSubtitle(@Nullable String subtitle) {
        mySubtitle = LocalizeValue.ofNullable(subtitle);
        return this;
    }

    public boolean hasContent() {
        return myContent.isNotEmpty();
    }

    
    public String getContent() {
        return myContent.get();
    }

    
    public Notification setContent(LocalizeValue content) {
        myContent = content;
        return this;
    }

    @Deprecated
    @DeprecationInfo("Use variant with LocalizeValue")
    
    public Notification setContent(@Nullable String content) {
        myContent = LocalizeValue.ofNullable(content);
        return this;
    }

    @Nullable
    public NotificationListener getListener() {
        return myListener;
    }

    
    public Notification setListener(NotificationListener listener) {
        myListener = listener;
        return this;
    }

    
    public List<AnAction> getActions() {
        return Lists.notNullize(myActions);
    }

    
    public static Notification get(AnActionEvent e) {
        //noinspection ConstantConditions
        return e.getData(KEY);
    }

    public static void fire(Notification notification, AnAction action) {
        fire(notification, action, null);
    }

    public static void fire(final Notification notification, AnAction action, @Nullable DataContext context) {
        AnActionEvent event = AnActionEvent.createFromAnAction(action, null, ActionPlaces.UNKNOWN, new DataContext() {
            @Nullable
            @Override
            @SuppressWarnings("unchecked")
            public <T> T getData(Key<T> dataId) {
                if (KEY == dataId) {
                    return (T) notification;
                }
                return context == null ? null : context.getData(dataId);
            }
        });

        NotificationActionInvoker.getInstance().invoke(action, event);
    }

    public static void setDataProvider(Notification notification, JComponent component) {
        DataManager.registerDataProvider(component, dataId -> KEY == dataId ? notification : null);
    }

    
    public String getDropDownText() {
        if (myDropDownText == null) {
            myDropDownText = "Actions";
        }
        return myDropDownText;
    }

    /**
     * @param dropDownText text for popup when all actions collapsed (when all actions width more notification width)
     */
    
    public Notification setDropDownText(String dropDownText) {
        myDropDownText = dropDownText;
        return this;
    }

    
    public CollapseActionsDirection getCollapseActionsDirection() {
        return myCollapseActionsDirection;
    }

    public void setCollapseActionsDirection(CollapseActionsDirection collapseActionsDirection) {
        myCollapseActionsDirection = collapseActionsDirection;
    }

    
    public Notification addActions(AnAction... actions) {
        for (AnAction action : actions) {
            addAction(action);
        }
        return this;
    }

    
    public Notification addAction(AnAction action) {
        if (myActions == null) {
            myActions = new ArrayList<>();
        }
        myActions.add(action);
        return this;
    }

    
    public NotificationType getType() {
        return myType;
    }

    public boolean isExpired() {
        return myExpired.get();
    }

    public void expire() {
        if (!myExpired.compareAndSet(false, true)) {
            return;
        }

        Application.get().getLastUIAccess().giveIfNeed(this::hideBalloon);
        NotificationsManager.getNotificationsManager().expire(this);

        Runnable whenExpired = myWhenExpired;
        if (whenExpired != null) {
            whenExpired.run();
        }
    }

    public Notification whenExpired(@Nullable Runnable whenExpired) {
        myWhenExpired = whenExpired;
        return this;
    }

    public void hideBalloon() {
        if (myBalloonRef != null) {
            Balloon balloon = myBalloonRef.get();
            if (balloon != null) {
                balloon.hide();
            }
            myBalloonRef = null;
        }
    }

    public void setBalloon(final Balloon balloon) {
        hideBalloon();
        myBalloonRef = new WeakReference<>(balloon);
        balloon.addListener(new JBPopupAdapter() {
            @Override
            public void onClosed(LightweightWindowEvent event) {
                if (SoftReference.dereference(myBalloonRef) == balloon) {
                    myBalloonRef = null;
                }
            }
        });
    }

    @Nullable
    public Balloon getBalloon() {
        return SoftReference.dereference(myBalloonRef);
    }

    @Deprecated
    @DeprecationInfo("Use Notification.Builder#notify()")
    public void notify(@Nullable Project project) {
        NotificationService.getInstance().notify(this, project);
    }

    public Notification setImportant(boolean important) {
        myImportant = important;
        return this;
    }

    public boolean isImportant() {
        if (myImportant != null) {
            return myImportant;
        }

        return getListener() != null || !ContainerUtil.isEmpty(myActions);
    }

    
    private String calculateId() {
        return myTimestamp + "." + System.identityHashCode(this);
    }

    private void assertHasTitleOrContent() {
        LOG.assertTrue(
            hasTitle() || hasContent(),
            "Notification should have title: [" + myTitle + "] and/or content: [" + myContent + "]; groupId: " + myGroupId
        );
    }
}
