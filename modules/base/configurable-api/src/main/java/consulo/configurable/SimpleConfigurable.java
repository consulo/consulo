/*
 * Copyright 2013-2016 consulo.io
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
package consulo.configurable;

import consulo.disposer.Disposable;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 22-Sep-16
 */
public abstract class SimpleConfigurable<T extends Supplier<? extends Component>> implements UnnamedConfigurable {
  private T myComponent;

  @RequiredUIAccess
  @Nonnull
  protected abstract T createPanel(@Nonnull Disposable uiDisposable);

  @RequiredUIAccess
  protected abstract boolean isModified(@Nonnull T component);

  @RequiredUIAccess
  protected abstract void apply(@Nonnull T component) throws ConfigurationException;

  @RequiredUIAccess
  protected abstract void reset(@Nonnull T component);

  @Nullable
  public final Component getPreferredFocusedComponent(@Nonnull T component) {
    return null;
  }

  @RequiredUIAccess
  protected void disposeUIResources(@Nonnull T component) {
    // nothing
  }

  @Override
  @Nullable
  public final Component getPreferredFocusedUIComponent() {
    if (myComponent != null) {
      return getPreferredFocusedComponent(myComponent);
    }
    return null;
  }

  @RequiredUIAccess
  @Nullable
  @Override
  public Component createUIComponent(@Nonnull Disposable uiDisposable) {
    if (myComponent == null) {
      myComponent = createPanel(uiDisposable);
    }
    return myComponent.get();
  }

  @Override
  @RequiredUIAccess
  public final boolean isModified() {
    return myComponent != null && isModified(myComponent);
  }

  @Override
  @RequiredUIAccess
  public final void apply() throws ConfigurationException {
    if (myComponent != null) {
      apply(myComponent);

      afterApply();
    }
  }

  protected void afterApply() {
  }

  @Override
  @RequiredUIAccess
  public final void reset() {
    if (myComponent != null) {
      reset(myComponent);
    }
  }

  @Override
  @RequiredUIAccess
  public final void disposeUIResources() {
    if (myComponent != null) {
      disposeUIResources(myComponent);

      myComponent = null;
    }
  }
}
