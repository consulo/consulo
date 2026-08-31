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
package consulo.ide.impl.fileEditor;

import consulo.codeEditor.EditorColors;
import consulo.colorScheme.EditorColorKey;
import consulo.colorScheme.EditorColorsManager;
import consulo.dataContext.DataManager;
import consulo.fileEditor.EditorNotificationBuilder;
import consulo.fileEditor.internal.EditorNotificationBuilderEx;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.Hyperlink;
import consulo.ui.Label;
import consulo.ui.NotificationType;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.TabsUtil;
import consulo.ui.border.BorderPosition;
import consulo.ui.border.BorderStyle;
import consulo.ui.event.ComponentEvent;
import consulo.ui.event.ComponentEventListener;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.impl.internal.action.ActionRunnerAsync;
import consulo.ui.image.Image;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 2026-08-13
 */
public class UnifiedEditorNotificationPanel implements EditorNotificationBuilderEx {
    private record ActionInfo(
        LocalizeValue text,
        LocalizeValue tooltipText,
        @Nullable Image icon,
        ComponentEventListener<Component, ComponentEvent<Component>> listener
    ) {
    }

    private final EditorColorsManager myEditorColorsManager;
    private final ActionManager myActionManager;
    private final DataManager myDataManager;

    private LocalizeValue myText = LocalizeValue.empty();
    private @Nullable Image myIcon;
    private EditorColorKey myBackgroundKey = EditorColors.NOTIFICATION_INFORMATION_BACKGROUND;
    private final List<ActionInfo> myActions = new ArrayList<>();
    private @Nullable ActionInfo myGearAction;

    private @Nullable DockLayout myRoot;

    public UnifiedEditorNotificationPanel(EditorColorsManager editorColorsManager, ActionManager actionManager, DataManager dataManager) {
        myEditorColorsManager = editorColorsManager;
        myActionManager = actionManager;
        myDataManager = dataManager;
    }

    @Override
    public EditorNotificationBuilder withText(LocalizeValue text) {
        myText = text;
        return this;
    }

    @Override
    public EditorNotificationBuilder withIcon(Image image) {
        myIcon = image;
        return this;
    }

    @Override
    public EditorNotificationBuilder withType(NotificationType notificationType) {
        switch (notificationType) {
            case INFO:
                myBackgroundKey = EditorColors.NOTIFICATION_INFORMATION_BACKGROUND;
                break;
            case WARNING:
                myBackgroundKey = EditorColors.NOTIFICATION_WARNING_BACKGROUND;
                break;
            case ERROR:
                myBackgroundKey = EditorColors.NOTIFICATION_ERROR_BACKGROUND;
                break;
            default:
                throw new IllegalArgumentException("Can't set " + notificationType);
        }
        return this;
    }

    @Override
    public EditorNotificationBuilder withAction(
        LocalizeValue actionText,
        LocalizeValue actionTooltipText,
        ComponentEventListener<Component, ComponentEvent<Component>> action
    ) {
        myActions.add(new ActionInfo(actionText, actionTooltipText, null, action));
        return this;
    }

    @Override
    public EditorNotificationBuilder withAction(LocalizeValue actionText, String actionRefId) {
        myActions.add(new ActionInfo(actionText, LocalizeValue.empty(), null, event -> executeAction(actionRefId, event.getComponent())));
        return this;
    }

    @Override
    public EditorNotificationBuilder withGearAction(
        LocalizeValue tooltipText,
        Image image,
        ComponentEventListener<Component, ComponentEvent<Component>> action
    ) {
        myGearAction = new ActionInfo(LocalizeValue.empty(), tooltipText, image, action);
        return this;
    }

    @Override
    @RequiredUIAccess
    public Component getUIComponent() {
        if (myRoot == null) {
            myRoot = build();
        }
        return myRoot;
    }

    @Override
    @RequiredUIAccess
    public Component getUIPreferredFocusableComponent() {
        return getUIComponent();
    }

    @Override
    public @Nullable Object getIntentionAction() {
        return null;
    }

    @Override
    public void dispose() {
    }

    @RequiredUIAccess
    private DockLayout build() {
        DockLayout root = DockLayout.create();

        Label label = Label.create(myText);
        label.setImage(myIcon);
        root.center(HorizontalLayout.create(0).add(label));

        HorizontalLayout links = HorizontalLayout.create(5);
        for (ActionInfo action : myActions) {
            links.add(createLink(action));
        }

        if (myGearAction != null) {
            links.add(createLink(myGearAction));
        }

        root.right(links);

        root.setMinHeight(TabsUtil.getTabsHeight());
        root.addBorder(BorderPosition.LEFT, BorderStyle.EMPTY, 10);
        root.addBorder(BorderPosition.RIGHT, BorderStyle.EMPTY, 10);

        root.setBackgroundColor(myEditorColorsManager.getGlobalScheme().getColor(myBackgroundKey));
        return root;
    }

    @RequiredUIAccess
    private Hyperlink createLink(ActionInfo action) {
        Hyperlink link = Hyperlink.create(action.text());
        link.setIcon(action.icon());
        if (!action.tooltipText().isEmpty()) {
            link.setToolTipText(action.tooltipText());
        }
        link.addHyperlinkListener(action.listener()::onEvent);
        return link;
    }

    @RequiredUIAccess
    private void executeAction(String actionId, Component component) {
        AnAction action = myActionManager.getAction(actionId);
        AnActionEvent event = new AnActionEvent(
            null,
            myDataManager.getDataContext(component),
            ActionPlaces.UNKNOWN,
            action.getTemplatePresentation(),
            myActionManager,
            0
        );
        UIAccess uiAccess = UIAccess.current();
        ActionRunnerAsync.lastUpdateAndCheckDumbAsync(action, event, true).whenCompleteAsync((enabled, throwable) -> {
            if (Boolean.TRUE.equals(enabled)) {
                action.actionPerformed(event);
            }
        }, uiAccess);
    }
}
