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
import org.jetbrains.annotations.NotNull;

/**
 * @author VISTALL
 * @since 19-Nov-16.
 */
public class WebServicesConfigurable extends SimpleConfigurableByProperties implements Configurable {
  @RequiredUIAccess
  @NotNull
  @Override
  protected Component createLayout(PropertyBuilder propertyBuilder) {
    UpdateSettings updateSettings = UpdateSettings.getInstance();

    VerticalLayout layout = Layouts.vertical();

    VerticalLayout repoLayout = Layouts.vertical();
    layout.add(Layouts.labeled("Repository settings").set(repoLayout));

    CheckBox enableUpdates = Components.checkBox("Enabled updates?");
    propertyBuilder.add(enableUpdates, updateSettings::isEnable, updateSettings::setEnable);

    ComboBox<UpdateChannel> channelComboBox = ComboBoxes.<UpdateChannel>simple().fillByEnum(UpdateChannel.class, Object::toString).build();
    channelComboBox.setEnabled(updateSettings.isEnable()); // set default state
    propertyBuilder.add(channelComboBox, updateSettings::getChannel, updateSettings::setChannel);
    enableUpdates.addValueListener(event -> channelComboBox.setEnabled(event.getValue()));

    repoLayout.add(Layouts.horizontal().add(enableUpdates));
    repoLayout.add(Labels.left("Channel: ", channelComboBox));

    WebServicesConfiguration webServicesConfiguration = WebServicesConfiguration.getInstance();
    for (WebServiceApi api : WebServiceApi.values()) {
      String description = api.getDescription();
      if (description == null) {
        continue;
      }

      TextField textField = Components.textField();

      layout.add(Layouts.labeled(description).set(Labels.leftFilled("OAuth Key: ", textField)));
      propertyBuilder.add(textField, () -> webServicesConfiguration.getOAuthKey(api), text -> webServicesConfiguration.setOAuthKey(api, text));
    }
    return layout;
  }
}
