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
package consulo.ide.impl.idea.notification.impl.actions;

import consulo.application.Application;
import consulo.application.dumb.DumbAware;
import consulo.ide.impl.idea.ide.util.PropertiesComponent;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.project.ui.notification.*;
import consulo.project.ui.notification.event.NotificationListener;
import consulo.project.ui.wm.ToolWindowId;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.MessageDialogBuilder;
import consulo.ui.image.Image;
import consulo.ui.image.ImageKey;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author spleaner
 * @author Sergey.Malenkov
 */
public class NotificationTestAction extends AnAction implements DumbAware {
    public static final NotificationGroup TEST_GROUP = NotificationGroup.balloonGroup("Test Ballon");
    public static final NotificationGroup TEST_STICKY_GROUP =
        new NotificationGroup("testSticky", LocalizeValue.localizeTODO("Test Sticky"), NotificationDisplayType.STICKY_BALLOON, true);
    public static final NotificationGroup TEST_TOOLWINDOW_GROUP =
        NotificationGroup.toolWindowGroup("testToolWindow", LocalizeValue.localizeTODO("Test ToolWindow"), ToolWindowId.TODO_VIEW, true);

    private static final String MESSAGE_KEY = "NotificationTestAction_Message";

    @RequiredUIAccess
    @Override
    public void actionPerformed(@Nonnull AnActionEvent event) {
        new NotificationDialog(event.getData(Project.KEY)).show();
    }

    private static final class NotificationDialog extends DialogWrapper {
        private final JTextArea myMessage = new JTextArea(10, 50);
        @Nullable
        private final Project myProject;

        private NotificationDialog(@Nullable Project project) {
            super(project, true, IdeModalityType.MODELESS);
            myProject = project;
            init();
            setOKButtonText(LocalizeValue.localizeTODO("Notify"));
            setTitle(LocalizeValue.localizeTODO("Test VcsBranchMappingChangedNotification"));
            myMessage.setText(
                PropertiesComponent.getInstance()
                    .getValue(MESSAGE_KEY, "GroupID:\nTitle:\nSubtitle:\nContent:\nType:\nActions:\nSticky:\n")
            );
        }

        @Nullable
        @Override
        protected String getDimensionServiceKey() {
            return "NotificationTestAction";
        }

        @Override
        protected JComponent createCenterPanel() {
            JPanel panel = new JPanel(new BorderLayout(10, 10));
            panel.add(BorderLayout.CENTER, new JScrollPane(myMessage));
            return panel;
        }

        @Nonnull
        @Override
        protected Action[] createActions() {
            return new Action[]{getOKAction(), getCancelAction()};
        }

        @Override
        public void doCancelAction() {
            PropertiesComponent.getInstance().setValue(MESSAGE_KEY, myMessage.getText());
            super.doCancelAction();
        }

        @Override
        protected void doOKAction() {
            newNotification(myMessage.getText());
        }

        private void newNotification(String text) {
            List<NotificationInfo> notifications = new ArrayList<>();
            NotificationInfo notification = null;

            for (String line : StringUtil.splitByLines(text, false)) {
                if (line.length() == 0) {
                    if (notification != null) {
                        notification = null;
                        continue;
                    }
                }
                if (line.startsWith("//")) {
                    continue;
                }
                if (line.startsWith("--")) {
                    break;
                }
                if (notification == null) {
                    notification = new NotificationInfo();
                    notifications.add(notification);
                }
                if (line.startsWith("GroupID:")) {
                    notification.setGroupId(StringUtil.substringAfter(line, ":"));
                }
                else if (line.startsWith("Title:")) {
                    notification.setTitle(StringUtil.substringAfter(line, ":"));
                }
                else if (line.startsWith("Content:")) {
                    String value = StringUtil.substringAfter(line, ":");
                    if (value != null) {
                        notification.addContent(value);
                    }
                }
                else if (line.startsWith("Subtitle:")) {
                    notification.setSubtitle(StringUtil.substringAfter(line, ":"));
                }
                else if (line.startsWith("Actions:")) {
                    String value = StringUtil.substringAfter(line, ":");
                    if (value != null) {
                        notification.setActions(StringUtil.split(value, ","));
                    }
                }
                else if (line.startsWith("Type:")) {
                    notification.setType(StringUtil.substringAfter(line, ":"));
                }
                else if (line.startsWith("Sticky:")) {
                    notification.setSticky("true".equals(StringUtil.substringAfter(line, ":")));
                }
                else if (line.startsWith("Listener:")) {
                    notification.setAddListener("true".equals(StringUtil.substringAfter(line, ":")));
                }
                else if (line.startsWith("Toolwindow:")) {
                    notification.setToolwindow("true".equals(StringUtil.substringAfter(line, ":")));
                }
            }

            Application.get().executeOnPooledThread(() -> {
                for (NotificationInfo info : notifications) {
                    info.getNotification().notify(myProject);
                }
            });
        }
    }

    private static class NotificationInfo implements NotificationListener {
        private String myGroupId;
        private String myTitle;
        private String mySubtitle;
        private List<String> myContent;
        private List<String> myActions;
        private NotificationType myType = NotificationType.INFORMATION;
        private boolean mySticky;
        private boolean myAddListener;
        private boolean myToolwindow;

        private Notification myNotification;

        public Notification getNotification() {
            if (myNotification == null) {
                NotificationGroup displayId = mySticky ? TEST_STICKY_GROUP : TEST_GROUP;
                if (myToolwindow) {
                    displayId = TEST_TOOLWINDOW_GROUP;
                }
                Notification.Builder builder = NotificationService.getInstance()
                    .newOfType(displayId, myType)
                    .title(LocalizeValue.ofNullable(myTitle))
                    .subtitle(LocalizeValue.ofNullable(mySubtitle))
                    .content(LocalizeValue.ofNullable(StringUtil.join(myContent, "\n")))
                    .optionalHyperlinkListener(getListener());
                if (myActions != null) {
                    for (String action : myActions) {
                        builder.addAction(new MyAnAction(action));
                    }
                }
                myNotification = builder.create();
            }
            return myNotification;
        }

        @Nullable
        private NotificationListener getListener() {
            return myAddListener ? this : null;
        }

        public void setGroupId(@Nullable String groupId) {
            myGroupId = groupId;
        }

        public void setTitle(@Nullable String title) {
            myTitle = title;
        }

        public void setSubtitle(@Nullable String subtitle) {
            mySubtitle = subtitle;
        }

        public void setAddListener(boolean addListener) {
            myAddListener = addListener;
        }

        public void addContent(@Nonnull String content) {
            if (myContent == null) {
                myContent = new ArrayList<>();
            }
            myContent.add(content);
        }

        public void setActions(@Nonnull List<String> actions) {
            myActions = actions;
        }

        public void setSticky(boolean sticky) {
            mySticky = sticky;
        }

        public void setToolwindow(boolean toolwindow) {
            myToolwindow = toolwindow;
        }

        public void setType(@Nullable String type) {
            if ("info".equals(type)) {
                myType = NotificationType.INFORMATION;
            }
            else if ("error".equals(type)) {
                myType = NotificationType.ERROR;
            }
            else if ("warn".equals(type)) {
                myType = NotificationType.WARNING;
            }
        }

        @Override
        @RequiredUIAccess
        public void hyperlinkUpdate(@Nonnull Notification notification, @Nonnull HyperlinkEvent event) {
            if (MessageDialogBuilder.yesNo("VcsBranchMappingChangedNotification Listener", event.getDescription() + "      Expire?")
                .isYes()) {
                myNotification.expire();
                myNotification = null;
            }
        }

        private class MyAnAction extends AnAction {
            private MyAnAction(@Nullable String text) {
                if (text != null) {
                    try {
                        ImageKey imageKey = ImageKey.fromString(text, Image.DEFAULT_ICON_SIZE, Image.DEFAULT_ICON_SIZE);
                        getTemplatePresentation().setIcon(imageKey);
                    }
                    catch (Exception ignored) {
                    }

                    getTemplatePresentation().setText(text);
                }
            }

            @Override
            @RequiredUIAccess
            public void actionPerformed(@Nonnull AnActionEvent e) {
                Notification.get(e);
                if (MessageDialogBuilder.yesNo("AnAction", getTemplatePresentation().getText() + "      Expire?").isYes()) {
                    myNotification.expire();
                    myNotification = null;
                }
            }
        }
    }
}
