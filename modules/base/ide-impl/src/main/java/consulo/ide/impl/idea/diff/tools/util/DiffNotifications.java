/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.ide.impl.idea.diff.tools.util;

import consulo.diff.comparison.DiffTooBigException;
import consulo.ide.impl.idea.diff.util.TextDiffType;
import consulo.ide.impl.idea.openapi.diff.DiffBundle;
import consulo.ide.impl.idea.ui.EditorNotificationPanel;
import consulo.ui.ex.awt.HyperlinkLabel;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.color.ColorValue;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;

public class DiffNotifications {
  @Nonnull
  public static JPanel createInsertedContent() {
    return createNotification("Content added", TextDiffType.INSERTED.getColor(null));
  }

  @Nonnull
  public static JPanel createRemovedContent() {
    return createNotification("Content removed", TextDiffType.DELETED.getColor(null));
  }

  @Nonnull
  public static JPanel createEqualContents() {
    return createEqualContents(true, true);
  }

  @Nonnull
  public static JPanel createEqualContents(boolean equalCharsets, boolean equalSeparators) {
    if (!equalCharsets && !equalSeparators) {
      return createNotification(DiffBundle.message("diff.contents.have.differences.only.in.charset.and.line.separators.message.text"));
    }
    if (!equalSeparators) {
      return createNotification(DiffBundle.message("diff.contents.have.differences.only.in.line.separators.message.text"));
    }
    if (!equalCharsets) {
      return createNotification(DiffBundle.message("diff.contents.have.differences.only.in.charset.message.text"));
    }
    return createNotification(DiffBundle.message("diff.contents.are.identical.message.text"));
  }

  @Nonnull
  public static JPanel createError() {
    return createNotification("Can not calculate diff");
  }

  @Nonnull
  public static JPanel createOperationCanceled() {
    return createNotification("Can not calculate diff. Operation canceled.");
  }

  @Nonnull
  public static JPanel createDiffTooBig() {
    return createNotification("Can not calculate diff. " + DiffTooBigException.MESSAGE);
  }

  //
  // Impl
  //

  @Nonnull
  public static JPanel createNotification(@Nonnull String text) {
    return createNotification(text, null);
  }

  @Nonnull
  public static JPanel createNotification(@Nonnull String text, @Nullable final ColorValue background) {
    return createNotification(text, background, true);
  }

  @Nonnull
  public static JPanel createNotification(@Nonnull String text, @Nullable final ColorValue background, boolean showHideAction) {
    final EditorNotificationPanel panel = new EditorNotificationPanel(TargetAWT.to(background));
    panel.text(text);
    if (showHideAction) {
      HyperlinkLabel link = panel.createActionLabel("Hide", () -> panel.setVisible(false));
      link.setToolTipText("Hide this notification");
    }
    return panel;
  }
}
