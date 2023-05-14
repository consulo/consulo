// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.build.ui.event;

import consulo.build.ui.event.BuildEventsNls;
import consulo.build.ui.event.MessageEvent;

import jakarta.annotation.Nullable;

/**
 * @author Vladislav.Soroka
 */
public interface MessageEventResult extends EventResult {
  MessageEvent.Kind getKind();

  @Nullable
  @BuildEventsNls.Description
  default String getDetails() { return null; }
}
