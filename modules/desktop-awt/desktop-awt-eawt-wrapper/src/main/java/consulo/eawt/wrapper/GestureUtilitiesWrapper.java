/*
 * Copyright 2013-2022 consulo.io
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
package consulo.eawt.wrapper;

import com.apple.eawt.event.GestureListener;
import com.apple.eawt.event.GestureUtilities;
import consulo.eawt.wrapper.event.GestureListenerWrapper;

import javax.swing.*;

/**
 * @author VISTALL
 * @since 13/01/2022
 */
public class GestureUtilitiesWrapper {
  public static void addGestureListenerTo(JComponent component, GestureListenerWrapper wrapper) {
    GestureUtilities.addGestureListenerTo(component, (GestureListener)wrapper.getDelegate());
  }

  public static void removeGestureListenerFrom(JComponent component, GestureListenerWrapper wrapper) {
    GestureUtilities.removeGestureListenerFrom(component, (GestureListener)wrapper.getDelegate());
  }
}
