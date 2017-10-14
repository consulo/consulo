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
package consulo.ide.welcomeScreen;

import com.intellij.ui.Gray;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.UIUtil;

import java.awt.*;

/**
 * @author VISTALL
 * @since 14-Oct-17
 */
public interface WelcomeScreenConstants {
  // This is for border around recent projects, action cards and also lines separating header and footer from main contents.
  static final Color BORDER_COLOR = new JBColor(Gray._190, Gray._85);


  // There two are for caption of Recent Project and Action Cards
  static final Color CAPTION_BACKGROUND = new JBColor(Gray._210, Gray._75);
  static final Color CAPTION_FOREGROUND = new JBColor(Color.black, Gray._197);

  static Color getProjectsBackground() {
    return new JBColor(Gray.xFF, Gray.x39);
  }

  static Color getLinkNormalColor() {
    return new JBColor(Gray._0, Gray.xBB);
  }

  static Color getActionLinkSelectionColor() {
    return new JBColor(0xdbe5f5, 0x485875);
  }

  static Color getSeparatorColor() {
    return UIUtil.getBorderColor();
  }
}
