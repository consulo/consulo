// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.build.output;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author Vladislav.Soroka
 */
public interface BuildOutputInstantReader {
  @Nonnull
  Object getParentEventId();

  @Nullable
  //@NlsSafe
  String readLine();

  void pushBack();

  void pushBack(int numberOfLines);
}
