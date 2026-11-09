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
import consulo.project.ui.localize.ProjectUILocalize;
import consulo.ui.ex.toolWindow.ButtonDisplay;
import consulo.ui.ex.toolWindow.ToolWindowSettings;
import consulo.ui.CheckBox;
import consulo.ui.Component;
import consulo.ui.Toast;
import consulo.ui.ToastStyle;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.LabeledLayout;
import consulo.ui.layout.VerticalLayout;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-08-28
 */
@ExtensionImpl
public class ToolWindowSettingsConfigurable extends SimpleConfigurableByProperties implements ProjectConfigurable {
    private final Provider<ToolWindowSettings> mySettings;

    @Inject
    public ToolWindowSettingsConfigurable(Provider<ToolWindowSettings> settings) {
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

        Toast largeIconToast = Toast.create();
        largeIconToast.addStyle(ToastStyle.INFO);
        largeIconToast.addStyle(ToastStyle.LEFT_LINE);
        largeIconToast.setText(ProjectUILocalize.toolwindowSettingsLargeIconNote());
        largeIconToast.setVisible(false);
        displayLayout.add(largeIconToast);
        displayGroup.addValueListener(display -> largeIconToast.setVisible(display == ButtonDisplay.LARGE_ICON));

        displayLayout.add(displayGroup.newButton(ProjectUILocalize.toolwindowSettingsDisplayText(), ButtonDisplay.TEXT));
        root.add(LabeledLayout.create(ProjectUILocalize.toolwindowSettingsButtonsGroup(), displayLayout));

        propertyBuilder.add(displayGroup, settings::getButtonDisplay, settings::setButtonDisplay);

        return root;
    }

    @Override
    @RequiredUIAccess
    protected void apply(LayoutWrapper component) throws ConfigurationException {
        super.apply(component);

        UISettings.getInstance().fireUISettingsChanged();
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
