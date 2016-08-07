/*
 * Copyright 2013-2016 must-be.org
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
package consulo.ui.internal;

import com.intellij.openapi.util.EmptyRunnable;
import consulo.ui.RequiredUIThread;
import consulo.web.servlet.ui.GwtUIAccess;
import org.jetbrains.annotations.NotNull;

/**
 * @author VISTALL
 * @since 16-Jun-16
 */
public class WGwtModalWindowImpl extends WGwtWindowImpl {
  private Runnable myCallback = EmptyRunnable.getInstance();

  public WGwtModalWindowImpl() {
    myVisible = false;
  }

  @RequiredUIThread
  public void show(@NotNull Runnable callback) {
    myCallback = callback;

    setVisibleImpl(true);
  }

  @RequiredUIThread
  public void hide(boolean callCallback) {
    setVisibleImpl(false);

    if(callCallback) {
      myCallback.run();
    }
  }

  @Override
  @RequiredUIThread
  @Deprecated
  public void setVisible(boolean value) {
    throw new IllegalArgumentException("Use show() or hide()");
  }

  private void setVisibleImpl(boolean value) {
    if (myVisible == value) {
      return;
    }

    myVisible = value;

    if (myVisible) {
      GwtUIAccess gwtUIAccess = GwtUIAccess.get();
      gwtUIAccess.showModal(this);
    }
    else {
      disposeImpl();
    }

    markAsChanged();
  }

  public void disposeImpl() {
  }
}
