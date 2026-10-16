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
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ui.notification.event.NotificationListener;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.image.Image;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.Lists;
import consulo.util.collection.SmartList;
import consulo.util.dataholder.Key;
import consulo.util.lang.function.Functions;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * <p>Notification bean class contains <b>title:</b>subtitle, content (plain text or HTML) and actions.</p>
 *
 * <p>A notification is kept by {@link NotificationService} until it expires, shown as a toast over the window and
 * listed in the Notifications tool window. Notifying, expiring and every query can be done from any thread.</p>
 *
 * <p>Warning: be careful not to use the links in HTML content, use {@link #addAction(AnAction)}</p>
 *
 * <p>Construct new notifications starting from {@link NotificationService}:</p>
 *
 * <code>notificationService.newError(GROUP)
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
        private @Nullable Image myIcon = null;
        private @Nullable Boolean myImportant = null;
        private boolean mySuggestion;
        private boolean myRemoveWhenExpired;
        private @Nullable NotificationListener myListener = null;
        private @Nullable List<Function<Notification, ? extends AnAction>> myActionAdders = null;
        private @Nullable Runnable myWhenExpired = null;

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

        /**
         * A suggestion is listed apart from the timeline of events and stays until it is acted on or dismissed.
         */
        public Builder suggestion() {
            mySuggestion = true;
            return this;
        }

        /**
         * An expired notification leaves the Notifications tool window instead of staying there greyed out.
         */
        public Builder removeWhenExpired() {
            myRemoveWhenExpired = true;
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
            return new Notification(this);
        }

        private void init(Notification notification) {
            notification.setIcon(myIcon);
            notification.setTitle(myTitle);
            notification.setSubtitle(mySubtitle);
            notification.setContent(myContent);
            notification.mySuggestion = mySuggestion;
            notification.myRemoveWhenExpired = myRemoveWhenExpired;
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

    public static final Key<Notification> KEY = Key.create(Notification.class);

    public final String id;
    private final String myGroupId;

    private final NotificationType myType;

    private @Nullable Image myIcon;
    private LocalizeValue myTitle = LocalizeValue.empty();
    private LocalizeValue mySubtitle = LocalizeValue.empty();
    private LocalizeValue myContent = LocalizeValue.empty();

    private @Nullable NotificationListener myListener;
    private @Nullable List<AnAction> myActions;

    private final AtomicBoolean myExpired = new AtomicBoolean();
    private @Nullable Runnable myWhenExpired;
    private @Nullable Boolean myImportant;
    private boolean mySuggestion;
    private boolean myRemoveWhenExpired;
    private final long myTimestamp = System.currentTimeMillis();

    protected Notification(Builder notificationBuilder) {
        myGroupId = notificationBuilder.myGroup.getId();
        myType = notificationBuilder.myType;
        notificationBuilder.init(this);

        assertHasTitleOrContent();
        id = calculateId();
    }

    /**
     * Returns the time (in milliseconds since Jan 1, 1970) when the notification was created.
     */
    public long getTimestamp() {
        return myTimestamp;
    }

    public @Nullable Image getIcon() {
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

    public LocalizeValue getTitleValue() {
        return myTitle;
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

    public LocalizeValue getSubtitleValue() {
        return mySubtitle;
    }

    public @Nullable String getSubtitle() {
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

    public LocalizeValue getContentValue() {
        return myContent;
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

    public @Nullable NotificationListener getListener() {
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

    /**
     * Runs an action of a notification the way a click on its link does. The event carries the notification under
     * {@link #KEY} and whatever the context gives.
     */
    @RequiredUIAccess
    public static void fire(Notification notification, AnAction action, @Nullable DataContext context) {
        Application.get().getInstance(NotificationService.class).fire(notification, action, context);
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

    public boolean isSuggestion() {
        return mySuggestion;
    }

    public boolean isRemoveWhenExpired() {
        return myRemoveWhenExpired;
    }

    public boolean isExpired() {
        return myExpired.get();
    }

    /**
     * Hides the toast of the notification and greys it out in the Notifications tool window. Safe from any thread;
     * the expiration listener runs on the calling thread.
     */
    public void expire() {
        if (!myExpired.compareAndSet(false, true)) {
            return;
        }

        Application.get().getInstance(NotificationService.class).expire(this);

        Runnable whenExpired = myWhenExpired;
        if (whenExpired != null) {
            whenExpired.run();
        }
    }

    @Deprecated
    @DeprecationInfo("Use Notification.Builder#notify()")
    public void notify(@Nullable Project project) {
        Application.get().getInstance(NotificationService.class).notify(this, project);
    }

    public Notification whenExpired(@Nullable Runnable whenExpired) {
        myWhenExpired = whenExpired;
        return this;
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
