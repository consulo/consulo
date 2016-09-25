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
package consulo.options;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.UnnamedConfigurable;
import consulo.annotations.RequiredDispatchThread;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author VISTALL
 * @since 22-Sep-16
 */
public abstract class SimpleConfigurable<T extends JComponent> implements UnnamedConfigurable {
  private T myComponent;

  @RequiredDispatchThread
  protected abstract T createPanel();

  @RequiredDispatchThread
  protected abstract boolean isModified(@NotNull T component);

  @RequiredDispatchThread
  protected abstract void apply(@NotNull T component) throws ConfigurationException;

  @RequiredDispatchThread
  protected abstract void reset(@NotNull T component);

  @Nullable
  public final JComponent getPreferredFocusedComponent(@NotNull T component) {
    return null;
  }

  @RequiredDispatchThread
  protected void disposeUIResources(@NotNull T component) {
    // nothing
  }

  @Nullable
  // usage if implement Configurable.HoldPreferredFocusedComponent
  public final JComponent getPreferredFocusedComponent() {
    if(myComponent != null) {
      return getPreferredFocusedComponent(myComponent);
    }
    return null;
  }

  @Nullable
  @Override
  public final JComponent createComponent() {
    if (myComponent != null) {
      myComponent = createPanel();
    }
    return myComponent;
  }

  @Override
  public final boolean isModified() {
    return myComponent != null && isModified(myComponent);
  }

  @Override
  public final void apply() throws ConfigurationException {
    if (myComponent != null) {
      apply(myComponent);
    }
  }

  @Override
  public final void reset() {
    if (myComponent != null) {
      reset(myComponent);
    }
  }

  @Override
  public final void disposeUIResources() {
    if (myComponent != null) {
      disposeUIResources(myComponent);

      myComponent = null;
    }
  }
}
