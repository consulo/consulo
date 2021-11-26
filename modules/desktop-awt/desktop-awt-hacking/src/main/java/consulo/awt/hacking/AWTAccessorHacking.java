/*
 * Copyright 2013-2020 consulo.io
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
package consulo.awt.hacking;

import sun.awt.AWTAccessor;

import java.awt.*;
import java.awt.peer.ComponentPeer;
import java.awt.peer.FramePeer;

/**
 * @author VISTALL
 * @since 2020-10-19
 */
public class AWTAccessorHacking {
  public static void setParent(Component comp, Container parent) {
    AWTAccessor.getComponentAccessor().setParent(comp, null);
  }

  public static void setGraphicsConfiguration(Component component, GraphicsConfiguration gc) {
    AWTAccessor.getComponentAccessor().setGraphicsConfiguration(component, gc);
  }

  public static Object getPeer(Component component) {
    return AWTAccessor.getComponentAccessor().getPeer(component);
  }

  public static int getExtendedStateFromPeer(Frame component) {
    int extendedState = component.getExtendedState();
    ComponentPeer peer = AWTAccessor.getComponentAccessor().getPeer(component);
    if (peer instanceof FramePeer) {
      // frame.state is not updated by jdk so get it directly from peer
      extendedState = ((FramePeer)peer).getState();
    }

    return extendedState;
  }
}
