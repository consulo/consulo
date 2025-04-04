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
package consulo.desktop.awt.ui.impl.validableComponent;

import consulo.disposer.Disposable;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.HasValidator;
import consulo.desktop.awt.ui.impl.base.SwingComponentDelegate;

import jakarta.annotation.Nonnull;
import java.awt.*;

/**
 * @author VISTALL
 * @since 2019-11-04
 */
public abstract class SwingValidableComponent<V, C extends Component> extends SwingComponentDelegate<C> implements HasValidator<V> {
  protected final SwingValidator<V> myValidator = new SwingValidator<>();

  protected abstract V getValue();

  @Nonnull
  @Override
  public Disposable addValidator(@Nonnull Validator<V> validator) {
    return myValidator.addValidator(validator);
  }

  @RequiredUIAccess
  @Override
  public boolean validate() {
    return myValidator.validateValue(toAWTComponent(), getValue(), false);
  }
}
