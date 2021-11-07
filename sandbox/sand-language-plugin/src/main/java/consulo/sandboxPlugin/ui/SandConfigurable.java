/*
 * Copyright 2013-2021 consulo.io
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
package consulo.sandboxPlugin.ui;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.security.impl.PrivilegedAction;
import consulo.ui.Alerts;
import consulo.ui.Button;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.VerticalLayout;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.net.URL;

/**
 * @author VISTALL
 * @since 30/10/2021
 */
public class SandConfigurable implements Configurable {
  @RequiredUIAccess
  @Nullable
  @Override
  public Component createUIComponent(@Nonnull Disposable parentDisposable) {
    VerticalLayout verticalLayout = VerticalLayout.create();

    verticalLayout.add(Button.create(LocalizeValue.localizeTODO("&Click me"), event -> {
      Alerts.okInfo(LocalizeValue.localizeTODO("Info")).showAsync(verticalLayout);
    }));

    verticalLayout.add(Button.create(LocalizeValue.localizeTODO("Open Me"), event -> {
      PrivilegedAction.runPrivilegedAction(() -> {
        try {
          return new URL("https://consulo.io").openStream();
        }
        catch (IOException e) {
        }
        return null;
      });

    }));

    return verticalLayout;
  }

  @RequiredUIAccess
  @Override
  public boolean isModified() {
    return false;
  }

  @RequiredUIAccess
  @Override
  public void apply() throws ConfigurationException {

  }
}
