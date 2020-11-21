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
package consulo.ui.web.internal;

import com.intellij.util.EventDispatcher;
import com.vaadin.event.FieldEvents;
import com.vaadin.ui.Component;
import consulo.disposer.Disposable;
import consulo.ui.FocusManager;
import consulo.ui.event.GlobalFocusListener;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2020-11-21
 */
public class WebFocusManagerImpl implements FocusManager {
  public static void register(Component component) {
    if(component instanceof FieldEvents.FocusNotifier) {
      ((FieldEvents.FocusNotifier)component).addFocusListener(focusEvent -> ourInstance.fireChanged());
    }

    if(component instanceof FieldEvents.BlurNotifier) {
      ((FieldEvents.BlurNotifier)component).addBlurListener(blurEvent -> ourInstance.fireChanged());
    }
  }

  public static final WebFocusManagerImpl ourInstance = new WebFocusManagerImpl();

  private final EventDispatcher<GlobalFocusListener> myListeners = EventDispatcher.create(GlobalFocusListener.class);

  public void fireChanged() {
    myListeners.getMulticaster().focusChanged();
  }

  @Nonnull
  @Override
  public Disposable addListener(@Nonnull GlobalFocusListener focusListener) {
    Disposable disposable = Disposable.newDisposable();
    myListeners.addListener(focusListener, disposable);
    return disposable;
  }
}
