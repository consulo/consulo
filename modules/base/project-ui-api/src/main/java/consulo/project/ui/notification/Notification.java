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

import consulo.application.Application;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ui.internal.NotificationActionInvoker;
import consulo.project.ui.notification.event.NotificationListener;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.popup.Balloon;
import consulo.ui.ex.popup.event.JBPopupAdapter;
import consulo.ui.ex.popup.event.LightweightWindowEvent;
import consulo.ui.image.Image;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.Lists;
import consulo.util.dataholder.Key;
import consulo.util.lang.StringUtil;
import consulo.util.lang.ref.SoftReference;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Notification bean class contains <b>title:</b>subtitle, content (plain text or HTML) and actions.
 * <br><br>
 * The notifications, generally, are shown in the balloons that appear on the screen when the corresponding events take place.<br>
 * Notification balloon is of two types: two or three lines.<br>
 * Two lines: title and content line; title and actions; content line and actions; contents on two lines.<br>
 * Three lines: title and content line and actions; contents on two lines and actions; contents on three lines or more; etc.
 * <br><br>
 * Warning: be careful not to use the links in HTML content, use {@link #addAction(AnAction)}
 *
 * @author spleaner
 * @author Alexander Lobas
 */
public class Notification {
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
    private Image myIcon;
    private final NotificationType myType;

    private String myTitle;
    private String mySubtitle;
    private String myContent;
    private NotificationListener myListener;
    private String myDropDownText;
    private List<AnAction> myActions;
    private CollapseActionsDirection myCollapseActionsDirection = CollapseActionsDirection.KEEP_RIGHTMOST;

    private AtomicReference<Boolean> myExpired = new AtomicReference<>(false);
    private Runnable myWhenExpired;
    private Boolean myImportant;
    private WeakReference<Balloon> myBalloonRef;
    private final long myTimestamp;

    public Notification(@Nonnull NotificationGroup group, @Nonnull String title, @Nonnull String content, @Nonnull NotificationType type) {
        this(group, title, content, type, null);
    }

    /**
     * @param group    notification grouo
     * @param icon     notification icon, if <b>null</b> used icon from type
     * @param title    notification title
     * @param subtitle notification subtitle
     * @param content  notification content
     * @param type     notification type
     * @param listener notification lifecycle listener
     */
    public Notification(
        @Nonnull NotificationGroup group,
        @Nullable Image icon,
        @Nullable String title,
        @Nullable String subtitle,
        @Nullable String content,
        @Nonnull NotificationType type,
        @Nullable NotificationListener listener
    ) {
        myGroupId = group.getId();
        myTitle = StringUtil.notNullize(title);
        myContent = StringUtil.notNullize(content);
        myType = type;
        myListener = listener;
        myTimestamp = System.currentTimeMillis();

        myIcon = icon;
        mySubtitle = subtitle;

        assertHasTitleOrContent();
        id = calculateId(this);
    }

    /**
     * @param group    notification group
     * @param title    notification title
     * @param content  notification content
     * @param type     notification type
     * @param listener notification lifecycle listener
     */
    public Notification(
        @Nonnull NotificationGroup group,
        @Nonnull String title,
        @Nonnull String content,
        @Nonnull NotificationType type,
        @Nullable @RequiredUIAccess NotificationListener listener
    ) {
        myGroupId = group.getId();
        myTitle = title;
        myContent = content;
        myType = type;
        myListener = listener;
        myTimestamp = System.currentTimeMillis();

        assertHasTitleOrContent();
        id = calculateId(this);
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

    @Nonnull
    public Notification setIcon(@Nullable Image icon) {
        myIcon = icon;
        return this;
    }

    @Nonnull
    public String getGroupId() {
        return myGroupId;
    }

    public boolean hasTitle() {
        return !StringUtil.isEmptyOrSpaces(myTitle) || !StringUtil.isEmptyOrSpaces(mySubtitle);
    }

    @Nonnull
    public String getTitle() {
        return myTitle;
    }

    @Nonnull
    public Notification setTitle(@Nullable String title) {
        myTitle = StringUtil.notNullize(title);
        return this;
    }

    @Nonnull
    public Notification setTitle(@Nullable String title, @Nullable String subtitle) {
        return setTitle(title).setSubtitle(subtitle);
    }

    @Nullable
    public String getSubtitle() {
        return mySubtitle;
    }

    @Nonnull
    public Notification setSubtitle(@Nullable String subtitle) {
        mySubtitle = subtitle;
        return this;
    }

    public boolean hasContent() {
        return !StringUtil.isEmptyOrSpaces(myContent);
    }

    @Nonnull
    public String getContent() {
        return myContent;
    }

    @Nonnull
    public Notification setContent(@Nullable String content) {
        myContent = StringUtil.notNullize(content);
        return this;
    }

    @Nullable
    public NotificationListener getListener() {
        return myListener;
    }

    @Nonnull
    public Notification setListener(@Nonnull NotificationListener listener) {
        myListener = listener;
        return this;
    }

    @Nonnull
    public List<AnAction> getActions() {
        return Lists.notNullize(myActions);
    }

    @Nonnull
    public static Notification get(@Nonnull AnActionEvent e) {
        //noinspection ConstantConditions
        return e.getData(KEY);
    }

    public static void fire(@Nonnull final Notification notification, @Nonnull AnAction action) {
        fire(notification, action, null);
    }

    public static void fire(@Nonnull final Notification notification, @Nonnull AnAction action, @Nullable DataContext context) {
        AnActionEvent event = AnActionEvent.createFromAnAction(action, null, ActionPlaces.UNKNOWN, new DataContext() {
            @Nullable
            @Override
            @SuppressWarnings("unchecked")
            public <T> T getData(@Nonnull Key<T> dataId) {
                if (KEY == dataId) {
                    return (T) notification;
                }
                return context == null ? null : context.getData(dataId);
            }
        });

        NotificationActionInvoker.getInstance().invoke(action, event);
    }

    public static void setDataProvider(@Nonnull Notification notification, @Nonnull JComponent component) {
        DataManager.registerDataProvider(component, dataId -> KEY == dataId ? notification : null);
    }

    @Nonnull
    public String getDropDownText() {
        if (myDropDownText == null) {
            myDropDownText = "Actions";
        }
        return myDropDownText;
    }

    /**
     * @param dropDownText text for popup when all actions collapsed (when all actions width more notification width)
     */
    @Nonnull
    public Notification setDropDownText(@Nonnull String dropDownText) {
        myDropDownText = dropDownText;
        return this;
    }

    @Nonnull
    public CollapseActionsDirection getCollapseActionsDirection() {
        return myCollapseActionsDirection;
    }

    public void setCollapseActionsDirection(@Nonnull CollapseActionsDirection collapseActionsDirection) {
        myCollapseActionsDirection = collapseActionsDirection;
    }

    @Nonnull
    public Notification addActions(@Nonnull AnAction... actions) {
        for (AnAction action : actions) {
            addAction(action);
        }
        return this;
    }

    @Nonnull
    public Notification addAction(@Nonnull AnAction action) {
        if (myActions == null) {
            myActions = new ArrayList<>();
        }
        myActions.add(action);
        return this;
    }

    @Nonnull
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

        Application.get().getLastUIAccess().giveIfNeed(() -> hideBalloon());
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
            final Balloon balloon = myBalloonRef.get();
            if (balloon != null) {
                balloon.hide();
            }
            myBalloonRef = null;
        }
    }

    public void setBalloon(@Nonnull final Balloon balloon) {
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

    public void notify(@Nullable Project project) {
        Notifications.Bus.notify(this, project);
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

    @Nonnull
    private static String calculateId(@Nonnull Object notification) {
        return String.valueOf(System.currentTimeMillis()) + "." + String.valueOf(System.identityHashCode(notification));
    }

    private void assertHasTitleOrContent() {
        LOG.assertTrue(
            hasTitle() || hasContent(),
            "Notification should have title: [" + myTitle + "] and/or content: [" + myContent + "]; groupId: " + myGroupId
        );
    }
}
