/*
 * Copyright 2013-2021 consulo.io
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
package consulo.ui.dialog;

import com.intellij.openapi.actionSystem.AnAction;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.Size;
import consulo.ui.dialog.action.CancelAction;
import consulo.ui.dialog.action.OkAction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 13/12/2021
 */
public abstract class DialogDescriptor<V> {
  private final LocalizeValue myTitle;

  public DialogDescriptor(@Nonnull LocalizeValue title) {
    myTitle = title;
  }

  @Nullable
  public Size getInitialSize() {
    return null;
  }

  @Nonnull
  public abstract Component createCenterComponent(@Nonnull Disposable uiDisposable);

  @Nullable
  public V getOkValue() {
    return null;
  }

  @Nonnull
  public AnAction[] createActions(boolean inverseOrder) {
    if (inverseOrder) {
      return new AnAction[]{new CancelAction(), new OkAction()};
    }
    else {
      return new AnAction[]{new OkAction(), new CancelAction()};
    }
  }

  public boolean isSetDefaultContentBorder() {
    return true;
  }

  @Nonnull
  public LocalizeValue getTitle() {
    return myTitle;
  }

  public boolean isDefaultAction(AnAction action) {
    return action instanceof OkAction;
  }
}
