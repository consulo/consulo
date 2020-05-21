/*
 * Copyright 2013-2019 consulo.io
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
package consulo.ui;

import consulo.disposer.Disposable;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2019-11-04
 */
public interface ValidableComponent<V> extends Component {
  static class ValidationInfo {
    private final String myMessage;
    private final Image myIcon;
    private final NotificationType myType;

    public ValidationInfo(@Nonnull String message) {
      this(message, null, NotificationType.ERROR);
    }

    public ValidationInfo(@Nonnull String message, @Nullable Image icon, @Nonnull NotificationType type) {
      myMessage = message;
      myIcon = icon;
      myType = type;
    }

    @Nonnull
    public String getMessage() {
      return myMessage;
    }

    @Nullable
    public Image getIcon() {
      return myIcon;
    }

    @Nonnull
    public NotificationType getType() {
      return myType;
    }
  }

  interface Validator<V> {
    @Nullable
    ValidationInfo validateValue(V value);
  }

  @Nonnull
  Disposable addValidator(@Nonnull Validator<V> validator);

  /**
   * Check value in all validaters and show notification
   *
   * @return true if all validators return null
   */
  @RequiredUIAccess
  boolean validate();
}
