// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.util;

import javax.annotation.Nullable;

import java.awt.*;

/**
 * @author gregsh
 */
public interface ColoredItem {
  @Nullable
  Color getColor();
}
