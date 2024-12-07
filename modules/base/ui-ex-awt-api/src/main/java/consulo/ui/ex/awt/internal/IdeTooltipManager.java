/*
 * Copyright 2013-2022 consulo.io
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
package consulo.ui.ex.awt.internal;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.ui.ex.awt.hint.HintHint;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nullable;
import javax.swing.*;
import java.awt.*;

/**
 * @author VISTALL
 * @since 30-Apr-22
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface IdeTooltipManager {
  static IdeTooltipManager getInstance() {
    return Application.get().getInstance(IdeTooltipManager.class);
  }

  Color getLinkForeground(boolean awtTooltip);

  boolean isOwnBorderAllowed(boolean awtTooltip);

  Color getBorderColor(boolean awtTooltip);

  Color getTextBackground(boolean awtTooltip);

  Color getTextForeground(boolean awtTooltip);

  boolean isOpaqueAllowed(boolean awtTooltip);

  Font getTextFont(boolean awtTooltip);                                                                

  String getUlImg(boolean awtTooltip);

  JEditorPane initEditorPane(@NonNls String text, final HintHint hintHint, @Nullable final JLayeredPane layeredPane);
}
