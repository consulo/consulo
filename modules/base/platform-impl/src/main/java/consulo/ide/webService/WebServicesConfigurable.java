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
package consulo.ide.webService;

import com.intellij.openapi.options.Configurable;
import consulo.ide.updateSettings.UpdateChannel;
import consulo.ide.updateSettings.UpdateSettings;
import consulo.options.SimpleConfigurableByProperties;
import consulo.ui.*;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.LabeledLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.util.LabeledComponents;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 19-Nov-16.
 */
public class WebServicesConfigurable extends SimpleConfigurableByProperties implements Configurable {
  @RequiredUIAccess
  @Nonnull
  @Override
  protected Component createLayout(PropertyBuilder propertyBuilder) {
    UpdateSettings updateSettings = UpdateSettings.getInstance();

    VerticalLayout layout = VerticalLayout.create();

    VerticalLayout repoLayout = VerticalLayout.create();
    layout.add(LabeledLayout.create("Repository settings", repoLayout));

    CheckBox enableUpdates = CheckBox.create("Enabled updates?");
    propertyBuilder.add(enableUpdates, updateSettings::isEnable, updateSettings::setEnable);

    ComboBox<UpdateChannel> channelComboBox = ComboBox.<UpdateChannel>builder().fillByEnum(UpdateChannel.class, Object::toString).build();
    channelComboBox.setEnabled(updateSettings.isEnable()); // set default state
    propertyBuilder.add(channelComboBox, updateSettings::getChannel, updateSettings::setChannel);
    enableUpdates.addValueListener(event -> channelComboBox.setEnabled(event.getValue()));

    repoLayout.add(HorizontalLayout.create().add(enableUpdates));
    repoLayout.add(LabeledComponents.left("Channel", channelComboBox));

    WebServicesConfiguration webServicesConfiguration = WebServicesConfiguration.getInstance();
    for (WebServiceApi api : WebServiceApi.values()) {
      String description = api.getDescription();
      if (description == null) {
        continue;
      }

      TextBox textBox = TextBox.create();

      layout.add(LabeledLayout.create(description, LabeledComponents.leftFilled("OAuth Key", textBox)));
      propertyBuilder.add(textBox, () -> webServicesConfiguration.getOAuthKey(api), text -> webServicesConfiguration.setOAuthKey(api, text));
    }
    return layout;
  }
}
