/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.roots.ui.util;

import consulo.ide.ui.CellAppearanceEx;
import consulo.ui.ex.ColoredTextContainer;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.image.Image;
import consulo.util.lang.StringUtil;

import jakarta.annotation.Nonnull;

public abstract class BaseTextCommentCellAppearance implements CellAppearanceEx {
  protected abstract Image getIcon();

  protected abstract String getSecondaryText();

  protected abstract String getPrimaryText();

  @Override
  public void customize(@Nonnull final ColoredTextContainer component) {
    component.setIcon(getIcon());
    component.append(getPrimaryText(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
    final String secondaryText = getSecondaryText();
    if (!StringUtil.isEmptyOrSpaces(secondaryText)) {
      component.append(" (" + secondaryText + ")", SimpleTextAttributes.GRAY_ATTRIBUTES);
    }
  }

  @Nonnull
  public String getText() {
    String secondaryText = getSecondaryText();
    if (secondaryText != null && secondaryText.length() > 0) {
      return getPrimaryText() + " (" + secondaryText + ")";
    }
    return getPrimaryText();
  }
}
