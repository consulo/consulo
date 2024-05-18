// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.application.util;

import consulo.ui.color.ColorValue;
import jakarta.annotation.Nullable;

/**
 * @author gregsh
 *
 * TODO migrate to ui-api
 */
public interface ColoredItem {
  @Nullable
  ColorValue getColor();
}
