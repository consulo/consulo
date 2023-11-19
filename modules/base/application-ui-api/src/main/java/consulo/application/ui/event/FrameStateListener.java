// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.application.ui.event;

import consulo.application.ui.FrameStateManager;
import consulo.application.ui.wm.FocusableFrame;

import java.util.EventListener;

/**
 * Listener for receiving notifications when the IDE window is activated or deactivated.
 *
 * @since 5.0.2
 * @see FrameStateManager#addListener(FrameStateListener)
 * @see FrameStateManager#removeListener(FrameStateListener)
 */
public interface FrameStateListener extends EventListener {
  /**
   * Called when the IDE window is deactivated.
   * @param ideFrame
   */
  default void onFrameDeactivated(FocusableFrame ideFrame) {
  }

  /**
   * Called when the IDEA window is activated.
   */
  default void onFrameActivated() {
  }

  /**
   * @deprecated use {@link FrameStateListener} directly
   */
  @Deprecated
  abstract class Adapter implements FrameStateListener {
  }
}
