/*
 * Copyright 2013-2023 consulo.io
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
package consulo.ide.impl.idea.codeInsight.highlighting;

import consulo.codeEditor.Editor;
import consulo.codeEditor.markup.LineMarkerRenderer;
import consulo.ui.color.ColorValue;

import jakarta.annotation.Nonnull;
import java.awt.*;

/**
* @author VISTALL
* @since 14/05/2023
*/
public class DefaultLineMarkerRenderer implements LineMarkerRenderer {
  private static final int DEEPNESS = 0;
  private static final int THICKNESS = 1;
  private final ColorValue myColor;

  DefaultLineMarkerRenderer(@Nonnull ColorValue color) {
    myColor = color;
  }

  @Override
  public void paint(Editor editor, Graphics g, Rectangle r) {
//    see consulo.desktop.awt.language.editor.DesktopAWTIndentPass.getIndentColor()
//    replaced by new ui - migrated to indent pass
//    int height = r.height + editor.getLineHeight();
//    g.setColor(TargetAWT.to(myColor));
//    g.fillRect(r.x, r.y, THICKNESS, height);
//    g.fillRect(r.x + THICKNESS, r.y, DEEPNESS, THICKNESS);
//    g.fillRect(r.x + THICKNESS, r.y + height - THICKNESS, DEEPNESS, THICKNESS);
  }

  public ColorValue getColor() {
    return myColor;
  }
}
