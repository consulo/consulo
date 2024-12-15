/*
 * Copyright 2013-2016 consulo.io
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
package consulo.ide.impl.updateSettings;

import consulo.annotation.component.ExtensionImpl;
import consulo.configurable.ApplicationConfigurable;
import consulo.configurable.Configurable;
import consulo.configurable.SimpleConfigurableByProperties;
import consulo.configurable.StandardConfigurableIds;
import consulo.disposer.Disposable;
import consulo.externalService.update.UpdateChannel;
import consulo.externalService.update.UpdateSettings;
import consulo.ide.impl.localize.PluginLocalize;
import consulo.ui.CheckBox;
import consulo.ui.ComboBox;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.LabeledLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.util.LabeledBuilder;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2016-11-19
 */
@ExtensionImpl
public class UpdatesConfigurable extends SimpleConfigurableByProperties implements Configurable, ApplicationConfigurable {
    @Nonnull
    @Override
    public String getId() {
        return "updateSettings";
    }

    @Nonnull
    @Override
    public String getDisplayName() {
        return PluginLocalize.updateSettingsTitle().get();
    }

    @Nullable
    @Override
    public String getParentId() {
        return StandardConfigurableIds.PLATFORM_AND_PLUGINS_GROUP;
    }

    @RequiredUIAccess
    @Nonnull
    @Override
    protected Component createLayout(PropertyBuilder propertyBuilder, @Nonnull Disposable uiDisposable) {
        UpdateSettings updateSettings = UpdateSettings.getInstance();

        VerticalLayout layout = VerticalLayout.create();

        VerticalLayout repoLayout = VerticalLayout.create();
        layout.add(LabeledLayout.create(PluginLocalize.updateSettingsRepositorySettingsSection(), repoLayout));

        CheckBox enableUpdates = CheckBox.create(PluginLocalize.updateSettingsUpdatesEnabled());
        propertyBuilder.add(enableUpdates, updateSettings::isEnable, updateSettings::setEnable);

        ComboBox<UpdateChannel> channelComboBox =
            ComboBox.<UpdateChannel>builder().fillByEnum(UpdateChannel.class, it -> !it.isObsolete(), Object::toString).build();
        channelComboBox.setEnabled(updateSettings.isEnable()); // set default state
        propertyBuilder.add(channelComboBox, updateSettings::getChannel, updateSettings::setChannel);
        enableUpdates.addValueListener(event -> channelComboBox.setEnabled(event.getValue()));

        repoLayout.add(HorizontalLayout.create().add(enableUpdates));
        repoLayout.add(LabeledBuilder.sided(PluginLocalize.updateSettingsChannelLabel(), channelComboBox));
        return layout;
    }
}
