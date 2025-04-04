/*
 * Copyright 2013-2024 consulo.io
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
package consulo.application.ui;

import consulo.ui.Component;
import consulo.ui.HasFocus;
import consulo.ui.HasMnemonic;

/**
 * @author VISTALL
 * @since 06-Jul-24
 */
public class NonFocusableSetting {
  public static <C extends Component & HasFocus & HasMnemonic> void initFocusability(C component) {
    if (!UISettings.getShadowInstance().DISABLE_MNEMONICS_IN_CONTROLS) { // Or that won't be keyboard accessible at all
      component.setFocusable(false);
    }
  }
}
