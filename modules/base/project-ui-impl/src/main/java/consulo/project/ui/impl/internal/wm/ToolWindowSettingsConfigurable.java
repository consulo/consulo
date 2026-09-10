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
package consulo.project.ui.impl.internal.wm;

import consulo.ui.RadioGroup;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.ui.UISettings;
import consulo.configurable.ConfigurationException;
import consulo.configurable.ProjectConfigurable;
import consulo.configurable.SimpleConfigurableByProperties;
import consulo.configurable.StandardConfigurableIds;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.project.ui.localize.ProjectUILocalize;
import consulo.project.ui.internal.ToolWindowLayout;
import consulo.project.ui.internal.ToolWindowManagerEx;
import consulo.project.ui.internal.WindowManagerEx;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.ui.ex.toolWindow.ButtonDisplay;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.ui.ex.toolWindow.ToolWindowSettings;
import consulo.ui.AdvancedLabel;
import consulo.ui.CheckBox;
import consulo.ui.Component;
import consulo.ui.RadioButton;
import consulo.ui.TextAttribute;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.LabeledLayout;
import consulo.ui.layout.VerticalLayout;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 2026-08-28
 */
@ExtensionImpl
public class ToolWindowSettingsConfigurable extends SimpleConfigurableByProperties implements ProjectConfigurable {
    private final Project myProject;
    private final Provider<ToolWindowSettings> mySettings;

    @Inject
    public ToolWindowSettingsConfigurable(Project project, Provider<ToolWindowSettings> settings) {
        myProject = project;
        mySettings = settings;
    }

    @Override
    @RequiredUIAccess
    protected Component createLayout(PropertyBuilder propertyBuilder, Disposable uiDisposable) {
        ToolWindowSettings settings = mySettings.get();

        VerticalLayout root = VerticalLayout.create();

        CheckBox paintFocusBox = CheckBox.create(ProjectUILocalize.toolwindowSettingsHighlightSelectedButton());
        root.add(paintFocusBox);
        propertyBuilder.add(paintFocusBox, settings::isPaintFocus, settings::setPaintFocus);

        CheckBox showMnemonicBox = CheckBox.create(ProjectUILocalize.toolwindowSettingsShowMnemonic());
        root.add(showMnemonicBox);
        propertyBuilder.add(showMnemonicBox, settings::isShowMnemonic, settings::setShowMnemonic);

        CheckBox hideToolStripesBox = CheckBox.create(ProjectUILocalize.toolwindowSettingsHideToolWindowBars());
        root.add(hideToolStripesBox);
        propertyBuilder.add(hideToolStripesBox, settings::isHideToolStripes, settings::setHideToolStripes);

        CheckBox widescreenBox = CheckBox.create(ProjectUILocalize.toolwindowSettingsWidescreenLayout());
        root.add(widescreenBox);
        propertyBuilder.add(widescreenBox, settings::isWidescreenSupport, settings::setWidescreenSupport);

        CheckBox leftSplitBox = CheckBox.create(ProjectUILocalize.toolwindowSettingsSideBySideLeft());
        root.add(leftSplitBox);
        propertyBuilder.add(leftSplitBox, settings::isLeftHorizontalSplit, settings::setLeftHorizontalSplit);

        CheckBox rightSplitBox = CheckBox.create(ProjectUILocalize.toolwindowSettingsSideBySideRight());
        root.add(rightSplitBox);
        propertyBuilder.add(rightSplitBox, settings::isRightHorizontalSplit, settings::setRightHorizontalSplit);

        CheckBox alwaysShowButtonsBox = CheckBox.create(ProjectUILocalize.toolwindowSettingsAlwaysShowButtons());
        root.add(alwaysShowButtonsBox);
        propertyBuilder.add(alwaysShowButtonsBox, settings::isAlwaysShowWindowButtons, settings::setAlwaysShowWindowButtons);

        RadioGroup<ButtonDisplay> displayGroup = RadioGroup.create();

        VerticalLayout displayLayout = VerticalLayout.create();
        displayLayout.add(displayGroup.newButton(ProjectUILocalize.toolwindowSettingsDisplayIconAndText(), ButtonDisplay.ICON_AND_TEXT));
        displayLayout.add(displayGroup.newButton(ProjectUILocalize.toolwindowSettingsDisplayIcon(), ButtonDisplay.ICON));
        displayLayout.add(displayGroup.newButton(ProjectUILocalize.toolwindowSettingsDisplayLargeIcon(), ButtonDisplay.LARGE_ICON));
        AdvancedLabel largeIconNote = AdvancedLabel.create();
        largeIconNote.updatePresentation(presentation -> presentation.append(
            ProjectUILocalize.toolwindowSettingsLargeIconNote(),
            TextAttribute.GRAYED
        ));
        displayLayout.add(largeIconNote);
        displayLayout.add(displayGroup.newButton(ProjectUILocalize.toolwindowSettingsDisplayText(), ButtonDisplay.TEXT));
        root.add(LabeledLayout.create(ProjectUILocalize.toolwindowSettingsButtonsGroup(), displayLayout));

        propertyBuilder.add(displayGroup, settings::getButtonDisplay, settings::setButtonDisplay);

        return root;
    }

    @Override
    @RequiredUIAccess
    protected void apply(LayoutWrapper component) throws ConfigurationException {
        ToolWindowSettings settings = mySettings.get();
        ButtonDisplay oldDisplay = settings.getButtonDisplay();

        super.apply(component);

        ButtonDisplay newDisplay = settings.getButtonDisplay();
        if (oldDisplay != newDisplay && (oldDisplay == ButtonDisplay.LARGE_ICON || newDisplay == ButtonDisplay.LARGE_ICON)) {
            ToolWindowManager manager = ToolWindowManager.getInstance(myProject);
            List<String> visibleIds = new ArrayList<>();
            for (String id : manager.getToolWindowIds()) {
                ToolWindow toolWindow = manager.getToolWindow(id);
                if (toolWindow != null && toolWindow.isVisible()) {
                    visibleIds.add(id);
                }
            }

            resetLayout();

            for (String id : visibleIds) {
                ToolWindow toolWindow = manager.getToolWindow(id);
                if (toolWindow != null) {
                    toolWindow.show(null);
                }
            }
        }

        UISettings.getInstance().fireUISettingsChanged();
    }

    @RequiredUIAccess
    private void resetLayout() {
        ToolWindowLayout layout = WindowManagerEx.getInstanceEx().getLayout();
        ToolWindowManagerEx.getInstanceEx(myProject).setLayout(layout);
    }

    @Override
    public String getId() {
        return "project.toolwindow.settings";
    }

    @Override
    public LocalizeValue getDisplayName() {
        return ProjectUILocalize.toolwindowSettingsConfigurableDisplayName();
    }

    @Override
    public @Nullable String getParentId() {
        return StandardConfigurableIds.GENERAL_GROUP;
    }
}
