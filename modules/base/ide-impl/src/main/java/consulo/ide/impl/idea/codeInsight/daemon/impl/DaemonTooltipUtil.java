/*
 * Copyright 2000-2017 JetBrains s.r.o.
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

package consulo.ide.impl.idea.codeInsight.daemon.impl;

import consulo.codeEditor.Editor;
import consulo.ide.impl.idea.codeInsight.hint.TooltipController;
import consulo.language.editor.impl.internal.hint.EditorMouseHoverPopupManager;
import consulo.language.editor.impl.internal.hint.TooltipGroup;
import consulo.language.editor.impl.internal.rawHighlight.HighlightInfoImpl;

import jakarta.annotation.Nonnull;

/**
 * @author max
 */
public class DaemonTooltipUtil {
  private static final TooltipGroup DAEMON_INFO_GROUP = new TooltipGroup("DAEMON_INFO_GROUP", 0);

  public static void showInfoTooltip(HighlightInfoImpl info, Editor editor, int defaultOffset) {
    showInfoTooltip(info, editor, defaultOffset, -1);
  }

  public static void cancelTooltips() {
    TooltipController.getInstance().cancelTooltip(DAEMON_INFO_GROUP, null, true);
  }

  private static void showInfoTooltip(@Nonnull final HighlightInfoImpl info, @Nonnull Editor editor, final int defaultOffset, final int currentWidth) {
    showInfoTooltip(info, editor, defaultOffset, currentWidth, false, false);
  }

  static void showInfoTooltip(@Nonnull final HighlightInfoImpl info,
                              @Nonnull Editor editor,
                              final int defaultOffset,
                              final int currentWidth,
                              final boolean requestFocus,
                              final boolean showImmediately) {
    EditorMouseHoverPopupManager.getInstance().showInfoTooltip(editor, info, defaultOffset, requestFocus, showImmediately);
  }
}
