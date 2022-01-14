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

import com.apple.eawt.event.*;

/**
 * @author VISTALL
 * @since 14/01/2022
 */
public abstract class GestureAdapterWrapper extends GestureListenerWrapper {
  private GestureAdapter myAdapter = new GestureAdapter() {
    @Override
    public void gestureBegan(GesturePhaseEvent event) {
      GestureAdapterWrapper.this.gestureBegan(new GesturePhaseEventWrapper(event));
    }

    @Override
    public void gestureEnded(GesturePhaseEvent event) {
      GestureAdapterWrapper.this.gestureEnded(new GesturePhaseEventWrapper(event));
    }

    @Override
    public void magnify(MagnificationEvent event) {
      GestureAdapterWrapper.this.magnify(new MagnificationEventWrapper(event));
    }

    @Override
    public void rotate(RotationEvent event) {
      GestureAdapterWrapper.this.rotate(new RotationEventWrapper(event));
    }

    @Override
    public void swipedDown(SwipeEvent event) {
      GestureAdapterWrapper.this.swipedDown(new SwipeEventWrapper(event));
    }

    @Override
    public void swipedLeft(SwipeEvent event) {
      GestureAdapterWrapper.this.swipedLeft(new SwipeEventWrapper(event));
    }

    @Override
    public void swipedRight(SwipeEvent event) {
      GestureAdapterWrapper.this.swipedRight(new SwipeEventWrapper(event));
    }

    @Override
    public void swipedUp(SwipeEvent event) {
      GestureAdapterWrapper.this.swipedUp(new SwipeEventWrapper(event));
    }
  };

  public void gestureBegan(GesturePhaseEventWrapper event) {
  }

  public void gestureEnded(GesturePhaseEventWrapper event) {
  }

  public void magnify(MagnificationEventWrapper event) {
  }

  public void rotate(RotationEventWrapper event) {
  }

  public void swipedDown(SwipeEventWrapper event) {
  }

  public void swipedLeft(SwipeEventWrapper event) {
  }

  public void swipedRight(SwipeEventWrapper event) {
  }

  public void swipedUp(SwipeEventWrapper event) {
  }

  @Override
  public Object getDelegate() {
    return myAdapter;
  }
}
