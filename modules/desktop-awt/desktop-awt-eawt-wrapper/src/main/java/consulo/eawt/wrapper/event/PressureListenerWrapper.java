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
package consulo.eawt.wrapper.event;

import com.apple.eawt.event.PressureEvent;
import com.apple.eawt.event.PressureListener;

/**
 * @author VISTALL
 * @since 13/01/2022
 */
public abstract class PressureListenerWrapper extends GestureListenerWrapper {
  private final PressureListener myListener = new PressureListener() {
    @Override
    public void pressure(PressureEvent e) {
      PressureListenerWrapper.this.pressure(new PressureEventWrapper(e));
    }
  };

  public abstract void pressure(PressureEventWrapper event);

  @Override
  public Object getDelegate() {
    return myListener;
  }
}
