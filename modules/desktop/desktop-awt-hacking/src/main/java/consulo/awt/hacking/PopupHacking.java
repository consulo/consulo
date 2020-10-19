/*
 * Copyright 2013-2020 consulo.io
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
package consulo.awt.hacking;

import consulo.awt.hacking.util.FieldAccessor;

import javax.swing.*;
import java.awt.*;

/**
 * @author VISTALL
 * @since 2020-10-19
 */
public class PopupHacking {
  private static final FieldAccessor<Popup, Component> popupComponent = new FieldAccessor<>(Popup.class, "component");

  public static Component getComponent(Popup popup) {
    if(popupComponent.isAvailable()) {
      return popupComponent.get(popup);
    }
    return null;
  }
}
