/*
 * Copyright 2013-2017 consulo.io
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

package consulo.ide.impl.presentationAssistant;

import consulo.ui.ex.awt.JBUI;

/**
 * @author VISTALL
 * @since 21-Aug-17
 * <p>
 * original author Nikolay Chashnikov (kotlin)
 */
public class PresentationAssistantState {
  public boolean myShowActionDescriptions = false;
  public int myFontSize = 24;
  public Keymaps.KeymapDescription mainKeymap = Keymaps.getDefaultMainKeymap();
  public Keymaps.KeymapDescription alternativeKeymap = Keymaps.getDefaultAlternativeKeymap();

  public int getFontSize() {
    return JBUI.scaleFontSize(myFontSize);
  }
}
