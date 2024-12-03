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

import com.apple.eawt.FullScreenListener;
import com.apple.eawt.event.FullScreenEvent;

/**
 * @author VISTALL
 * @since 14/01/2022
 */
public abstract class FullScreenListenerWrapper {
  private FullScreenListener myFullScreenListener = new FullScreenListener() {
    @Override
    public void windowEnteringFullScreen(FullScreenEvent fullScreenEvent) {
      FullScreenListenerWrapper.this.windowEnteringFullScreen(new AppFullScreenEventWrapper(fullScreenEvent));
    }

    @Override
    public void windowEnteredFullScreen(FullScreenEvent fullScreenEvent) {
      FullScreenListenerWrapper.this.windowEnteredFullScreen(new AppFullScreenEventWrapper(fullScreenEvent));
    }

    @Override
    public void windowExitingFullScreen(FullScreenEvent fullScreenEvent) {
      FullScreenListenerWrapper.this.windowExitingFullScreen(new AppFullScreenEventWrapper(fullScreenEvent));
    }

    @Override
    public void windowExitedFullScreen(FullScreenEvent fullScreenEvent) {
      FullScreenListenerWrapper.this.windowExitedFullScreen(new AppFullScreenEventWrapper(fullScreenEvent));
    }
  };

  public void windowEnteringFullScreen(AppFullScreenEventWrapper event) {
  }

  public void windowEnteredFullScreen(AppFullScreenEventWrapper event) {
  }

  public void windowExitingFullScreen(AppFullScreenEventWrapper event) {
  }

  public void windowExitedFullScreen(AppFullScreenEventWrapper event) {
  }

  public Object getDelegate() {
    return myFullScreenListener;
  }
}
