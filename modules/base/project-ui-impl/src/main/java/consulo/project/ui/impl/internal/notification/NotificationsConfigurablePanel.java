/*
 * Copyright 2013-2026 consulo.io
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
package consulo.project.ui.impl.internal.notification;

import consulo.localize.LocalizeValue;
import consulo.project.ui.localize.ProjectUILocalize;
import consulo.project.ui.notification.NotificationDisplayType;
import consulo.project.ui.notification.NotificationGroup;
import consulo.ui.CheckBox;
import consulo.ui.ComboBox;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.LabelStyle;
import consulo.ui.ListBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.SystemNotifications;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.SplitLayoutPosition;
import consulo.ui.layout.TwoComponentSplitLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.util.FormBuilder;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * The notifications settings: whether toasts and system notifications show at all, then every group - its toast and
 * whether it is kept in the Notifications tool window.
 *
 * @author VISTALL
 * @since 2026-10-16
 */
public class NotificationsConfigurablePanel {
    private static class GroupSettings {
        private final LocalizeValue myDisplayName;
        private final NotificationSettings myOriginal;
        private NotificationSettings mySettings;

        GroupSettings(NotificationSettings settings, LocalizeValue displayName) {
            myOriginal = settings;
            mySettings = settings;
            myDisplayName = displayName;
        }
    }

    private final NotificationsConfigurationImpl myConfiguration;
    private final List<GroupSettings> myGroups = new ArrayList<>();

    private final CheckBox myDisplayBalloons = CheckBox.create(ProjectUILocalize.notificationSettingsDisplayBalloons());
    private final CheckBox mySystemNotifications = CheckBox.create(ProjectUILocalize.notificationSettingsSystem());
    private final ListBox<GroupSettings> myGroupList;
    private final Label myGroupTitle = Label.create();
    private final ComboBox<NotificationDisplayType> myDisplayType = ComboBox.create(NotificationDisplayType.values());
    private final CheckBox myLog = CheckBox.create(ProjectUILocalize.notificationSettingsLog());
    private final VerticalLayout myRoot = VerticalLayout.create();

    private @Nullable GroupSettings mySelected;
    private boolean myUpdating;

    @RequiredUIAccess
    public NotificationsConfigurablePanel(NotificationsConfigurationImpl configuration) {
        myConfiguration = configuration;

        for (NotificationSettings settings : configuration.getAllSettings()) {
            NotificationGroup group = NotificationGroupRegistrator.last().get(settings.getGroupId());
            myGroups.add(new GroupSettings(settings, group == null ? LocalizeValue.of(settings.getGroupId()) : group.getDisplayName()));
        }
        myGroups.sort(Comparator.comparing(it -> it.myDisplayName.get()));

        mySystemNotifications.setVisible(SystemNotifications.getInstance().isAvailable());

        myGroupList = ListBox.create(myGroups);
        myGroupList.setRenderer((presentation, item) -> presentation.append(Objects.requireNonNull(item.getValue()).myDisplayName.get()));
        myGroupList.addValueListener(event -> select(event.getValue()));

        myDisplayType.setTextRenderer(type -> type == null ? LocalizeValue.empty() : LocalizeValue.of(type.getTitle()));
        myDisplayType.addValueListener(event -> update());
        myLog.addValueListener(event -> update());

        myGroupTitle.addStyle(LabelStyle.FONT_BOLD);

        FormBuilder details = FormBuilder.create();
        details.addLabeled(ProjectUILocalize.notificationSettingsPopupType(), myDisplayType);
        details.addBottom(myLog);

        VerticalLayout detailsLayout = VerticalLayout.create();
        detailsLayout.add(myGroupTitle);
        detailsLayout.add(details.build());

        TwoComponentSplitLayout split = TwoComponentSplitLayout.create(SplitLayoutPosition.HORIZONTAL);
        split.setFirstComponent(myGroupList);
        split.setSecondComponent(DockLayout.create().top(detailsLayout));
        split.setProportion(40);

        myRoot.add(myDisplayBalloons);
        myRoot.add(mySystemNotifications);
        myRoot.add(split);

        reset();
        if (!myGroups.isEmpty()) {
            myGroupList.setValueByIndex(0);
        }
    }

    public Component getComponent() {
        return myRoot;
    }

    @RequiredUIAccess
    public void selectGroup(String text) {
        for (GroupSettings group : myGroups) {
            if (group.myDisplayName.get().toLowerCase().contains(text.toLowerCase())) {
                myGroupList.setValue(group);
                return;
            }
        }
    }

    @RequiredUIAccess
    private void select(@Nullable GroupSettings group) {
        mySelected = group;
        myUpdating = true;
        try {
            myGroupTitle.setText(group == null ? LocalizeValue.empty() : group.myDisplayName);
            myDisplayType.setEnabled(group != null);
            myLog.setEnabled(group != null);
            if (group != null) {
                myDisplayType.setValue(group.mySettings.getDisplayType());
                myLog.setValue(group.mySettings.isShouldLog());
            }
        }
        finally {
            myUpdating = false;
        }
    }

    @RequiredUIAccess
    private void update() {
        GroupSettings group = mySelected;
        if (myUpdating || group == null) {
            return;
        }
        NotificationDisplayType displayType = myDisplayType.getValue();
        group.mySettings = group.mySettings
            .withDisplayType(displayType == null ? NotificationDisplayType.BALLOON : displayType)
            .withShouldLog(Boolean.TRUE.equals(myLog.getValue()));
    }

    public boolean isModified() {
        for (GroupSettings group : myGroups) {
            if (!group.mySettings.equals(group.myOriginal)) {
                return true;
            }
        }
        return myConfiguration.SHOW_BALLOONS != Boolean.TRUE.equals(myDisplayBalloons.getValue())
            || myConfiguration.SYSTEM_NOTIFICATIONS != Boolean.TRUE.equals(mySystemNotifications.getValue());
    }

    public void apply() {
        for (GroupSettings group : myGroups) {
            myConfiguration.changeSettings(group.mySettings);
        }
        myConfiguration.SHOW_BALLOONS = Boolean.TRUE.equals(myDisplayBalloons.getValue());
        myConfiguration.SYSTEM_NOTIFICATIONS = Boolean.TRUE.equals(mySystemNotifications.getValue());
    }

    @RequiredUIAccess
    public void reset() {
        for (GroupSettings group : myGroups) {
            group.mySettings = NotificationsConfigurationImpl.getSettings(group.myOriginal.getGroupId());
        }
        myDisplayBalloons.setValue(myConfiguration.SHOW_BALLOONS);
        mySystemNotifications.setValue(myConfiguration.SYSTEM_NOTIFICATIONS);
        select(mySelected);
    }
}
