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
package consulo.ide.updateSettings;

import com.intellij.openapi.options.Configurable;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.options.SimpleConfigurableByProperties;
import consulo.ui.CheckBox;
import consulo.ui.ComboBox;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.LabeledLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.util.LabeledBuilder;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 19-Nov-16.
 */
public class UpdatesConfigurable extends SimpleConfigurableByProperties implements Configurable {
  @RequiredUIAccess
  @Nonnull
  @Override
  protected Component createLayout(PropertyBuilder propertyBuilder, @Nonnull Disposable uiDisposable) {
    UpdateSettings updateSettings = UpdateSettings.getInstance();

    VerticalLayout layout = VerticalLayout.create();

    VerticalLayout repoLayout = VerticalLayout.create();
    layout.add(LabeledLayout.create(LocalizeValue.localizeTODO("Repository settings"), repoLayout));

    CheckBox enableUpdates = CheckBox.create(LocalizeValue.localizeTODO("Enabled updates?"));
    propertyBuilder.add(enableUpdates, updateSettings::isEnable, updateSettings::setEnable);

    ComboBox<UpdateChannel> channelComboBox = ComboBox.<UpdateChannel>builder().fillByEnum(UpdateChannel.class, Object::toString).build();
    channelComboBox.setEnabled(updateSettings.isEnable()); // set default state
    propertyBuilder.add(channelComboBox, updateSettings::getChannel, updateSettings::setChannel);
    enableUpdates.addValueListener(event -> channelComboBox.setEnabled(event.getValue()));

    repoLayout.add(HorizontalLayout.create().add(enableUpdates));
    repoLayout.add(LabeledBuilder.sided(LocalizeValue.localizeTODO("Channel"), channelComboBox));
    return layout;
  }
}
