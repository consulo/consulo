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
package consulo.fileEditor.impl.internal.search;

import consulo.application.util.registry.Registry;
import consulo.find.FindInProjectSettings;
import consulo.find.localize.FindLocalize;
import consulo.localize.LocalizeValue;
import consulo.platform.Platform;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.TextArea;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.keymap.util.KeymapUtil;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.ListPopup;
import consulo.ui.ex.popup.SimpleListPopupStepBuilder;
import consulo.ui.style.ComponentColors;
import consulo.util.collection.ArrayUtil;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author VISTALL
 * @since 2026-10-02
 */
public class UnifiedSearchTextArea {
    private final @Nullable Project myProject;
    private final boolean mySearchMode;

    private final TextArea myTextArea;

    private final DefaultActionGroup mySuffixGroup = new DefaultActionGroup();
    private final ActionToolbar myPrefixToolbar;
    private final ActionToolbar mySuffixToolbar;

    private boolean myMultilineEnabled = true;
    private boolean myJustCleared;

    @RequiredUIAccess
    public UnifiedSearchTextArea(@Nullable Project project, boolean searchMode, Component targetComponent) {
        myProject = project;
        mySearchMode = searchMode;

        myTextArea = TextArea.create();
        myTextArea.setBackgroundColor(ComponentColors.COMPONENT_BACKGROUND);
        myTextArea.setMinRows(1);
        myTextArea.setMaxRows(Registry.get("ide.find.max.rows").asInteger());

        mySuffixGroup.add(new ClearAction());
        mySuffixGroup.add(new NewLineAction());

        mySuffixToolbar = createToolbar("SearchSuffixHistoryToolbar", mySuffixGroup, targetComponent);
        myTextArea.setSuffixComponent(mySuffixToolbar.getUIComponent());

        myPrefixToolbar = createToolbar(
            "SearchPrefixHistoryToolbar",
            ActionGroup.newImmutableBuilder().add(new ShowHistoryAction()).build(),
            targetComponent
        );
        myTextArea.setPrefixComponent(myPrefixToolbar.getUIComponent());

        myTextArea.addValueListener(event -> {
            String value = myTextArea.getValue();
            if (!StringUtil.isEmpty(value)) {
                myJustCleared = false;
            }

            if (!myMultilineEnabled && value != null && value.indexOf('\n') >= 0) {
                myTextArea.setValue(StringUtil.replace(value, "\n", " "));
            }
        });
    }

    private static ActionToolbar createToolbar(String place, ActionGroup group, Component targetComponent) {
        ActionToolbar toolbar = ActionToolbarFactory.getInstance().createActionToolbar(place, group, ActionToolbar.Style.INPLACE);
        toolbar.setTargetUIComponent(targetComponent);
        return toolbar;
    }

    public TextArea getTextArea() {
        return myTextArea;
    }

    public Component getComponent() {
        return myTextArea;
    }

    public boolean isJustCleared() {
        return myJustCleared;
    }

    @RequiredUIAccess
    public void setSuffixActions(List<? extends AnAction> actions) {
        mySuffixGroup.addAll(actions);

        mySuffixToolbar.updateActionsAsync();
    }

    @RequiredUIAccess
    public void updateExtraActions() {
        mySuffixToolbar.updateActionsAsync();
    }

    @RequiredUIAccess
    public CompletableFuture<?> updateAllAsync() {
        return CompletableFuture.allOf(myPrefixToolbar.updateActionsAsync(), mySuffixToolbar.updateActionsAsync());
    }

    public void setMultilineEnabled(boolean enabled) {
        myMultilineEnabled = enabled;
    }

    private class ShowHistoryAction extends DumbAwareAction {
        @RequiredUIAccess
        ShowHistoryAction() {
            super(
                mySearchMode ? FindLocalize.findSearchHistory() : FindLocalize.findReplaceHistory(),
                mySearchMode ? FindLocalize.findSearchHistory() : FindLocalize.findReplaceHistory(),
                PlatformIconGroup.actionsSearchwithhistory()
            );

            registerCustomShortcutSet(KeymapUtil.getActiveKeymapShortcuts("ShowSearchHistory"), myTextArea);
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(AnActionEvent e) {
            Project project = myProject != null ? myProject : e.getData(Project.KEY);
            if (project == null) {
                return;
            }

            FindInProjectSettings findInProjectSettings = FindInProjectSettings.getInstance(project);
            String[] recent = mySearchMode ? findInProjectSettings.getRecentFindStrings() : findInProjectSettings.getRecentReplaceStrings();
            if (recent.length == 0) {
                return;
            }

            SimpleListPopupStepBuilder<String> stepBuilder = SimpleListPopupStepBuilder.newBuilder(List.of(ArrayUtil.reverseArray(recent)));
            stepBuilder.withFinishAction(selectedValue -> {
                if (selectedValue != null) {
                    myTextArea.setValue(selectedValue);
                }
            });

            ListPopup popup = JBPopupFactory.getInstance().createListPopup(project, stepBuilder.build());
            popup.showUnderneathOf(e);
        }
    }

    private class ClearAction extends LegacyDumbAwareAction {
        ClearAction() {
            super(PlatformIconGroup.actionsCancel());
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(AnActionEvent e) {
            myJustCleared = !StringUtil.isEmpty(myTextArea.getValue());
            myTextArea.setValue("");
        }

        @Override
        @RequiredUIAccess
        public void update(AnActionEvent e) {
            e.getPresentation().setEnabledAndVisible(!StringUtil.isEmpty(myTextArea.getValue()));
        }
    }

    private class NewLineAction extends LegacyDumbAwareAction {
        @RequiredUIAccess
        NewLineAction() {
            super(FindLocalize.findNewLine(), LocalizeValue.empty(), PlatformIconGroup.actionsSearchnewline());

            getTemplatePresentation().setHoveredIcon(PlatformIconGroup.actionsSearchnewlinehover());

            int menuModifier = Platform.current().os().isMac() ? InputEvent.META_DOWN_MASK : InputEvent.CTRL_DOWN_MASK;
            registerCustomShortcutSet(
                new CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, menuModifier | InputEvent.SHIFT_DOWN_MASK)),
                myTextArea
            );
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(AnActionEvent e) {
            myTextArea.replaceSelection("\n");
            myTextArea.focus();
        }

        @Override
        @RequiredUIAccess
        public void update(AnActionEvent e) {
            e.getPresentation().setEnabledAndVisible(myMultilineEnabled);
        }
    }
}
