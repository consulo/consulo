// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.build;

import com.intellij.build.events.BuildEvent;
import javax.annotation.Nonnull;

/**
 * @author Vladislav.Soroka
 */
public interface BuildProgressListener {
  void onEvent(@Nonnull Object buildId, @Nonnull BuildEvent event);
}
