/*
 * Copyright 2013-2023 consulo.io
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

import java.awt.*;
import java.awt.event.InvocationEvent;

/**
 * @author VISTALL
 * @since 20/08/2023
 */
public class PotemkinEvents {
  public static boolean isUrgentInvocationEvent(AWTEvent event) {
    // LWCToolkit does 'invokeAndWait', which blocks native event processing until finished. The OS considers that blockage to be
    // app freeze, stops rendering UI and shows beach-ball cursor. We want the UI to act (almost) normally in write-action progresses,
    // so we let these specific events to be dispatched, hoping they wouldn't access project/code model.

    // problem (IDEA-192282): LWCToolkit event might be posted before PotemkinProgress appears,
    // and it then just sits in the queue blocking the whole UI until the progress is finished.

    return event.toString().contains(",runnable=sun.lwawt.macosx.LWCToolkit") || event instanceof MyInvocationEvent;
  }

  public static void invokeLaterNotBlocking(Object source, Runnable runnable) {
    Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(new MyInvocationEvent(source, runnable));
  }

  private static class MyInvocationEvent extends InvocationEvent {
    MyInvocationEvent(Object source, Runnable runnable) {
      super(source, runnable);
    }
  }
}
