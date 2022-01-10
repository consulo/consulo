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
package consulo.ui.app.impl.settings;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ex.ConfigurableWrapper;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.border.BorderPosition;
import consulo.ui.border.BorderStyle;
import consulo.ui.layout.ScrollableLayout;

/**
 * @author VISTALL
 * @since 27/04/2021
 */
public class UnifiedConfigurableContext implements Disposable {
  private Component myComponent;

  @RequiredUIAccess
  public UnifiedConfigurableContext(Configurable configurable) {
    Component uiComponent = configurable.createUIComponent(this);
    if (uiComponent != null) {
      configurable.reset();

      if(!ConfigurableWrapper.isNoMargin(configurable)) {
          uiComponent.addBorder(BorderPosition.TOP, BorderStyle.EMPTY, 5);
          uiComponent.addBorder(BorderPosition.BOTTOM, BorderStyle.EMPTY, 5);
          uiComponent.addBorder(BorderPosition.LEFT, BorderStyle.EMPTY, 8);
          uiComponent.addBorder(BorderPosition.RIGHT, BorderStyle.EMPTY, 8);
      }

      if (ConfigurableWrapper.isNoScroll(configurable)) {
        myComponent = uiComponent;
      }
      else {
        myComponent = ScrollableLayout.create(uiComponent);
      }
    }
    else {
      myComponent = Label.create(LocalizeValue.localizeTODO("Not supported UI"));
    }
  }

  public Component getComponent() {
    return myComponent;
  }

  @Override
  public void dispose() {

  }
}
