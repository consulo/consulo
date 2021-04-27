/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package com.intellij.openapi.options;

import consulo.annotation.DeprecationInfo;
import consulo.disposer.Disposable;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;

/**
 * Component which provides a configuration user interface.
 *
 * @author lesya
 * @see consulo.options.SimpleConfigurable
 */
public interface UnnamedConfigurable {
  /**
   * Returns the user interface component for editing the configuration.
   *
   * @param parentDisposable disposable parameter, per ui page, dispose on ui close
   * @return the component instance.
   */
  @Nullable
  @RequiredUIAccess
  @SuppressWarnings("deprecation")
  default JComponent createComponent(@Nonnull Disposable parentDisposable) {
    return createComponent();
  }

  /**
   * Returns the user interface component for editing the configuration.
   *
   * @return the component instance.
   */
  @Nullable
  @RequiredUIAccess
  @Deprecated(forRemoval = true)
  @DeprecationInfo("Use with Disposable parameter")
  default JComponent createComponent() {
    return null;
  }

  /**
   * Returns the user interface component for editing the configuration.
   *
   * @param parentDisposable disposable parameter, per ui page, dispose on ui close
   * @return the component instance.
   */
  @Nullable
  @RequiredUIAccess
  @SuppressWarnings("deprecation")
  default Component createUIComponent(@Nonnull Disposable parentDisposable) {
    return createUIComponent();
  }

  @Nullable
  @RequiredUIAccess
  @Deprecated(forRemoval = true)
  @DeprecationInfo("Use with Disposable parameter")
  default Component createUIComponent() {
    return null;
  }

  /**
   * Checks if the settings in the user interface component were modified by the user and
   * need to be saved.
   *
   * @return true if the settings were modified, false otherwise.
   */
  @RequiredUIAccess
  boolean isModified();

  /**
   * Store the settings from configurable to other components.
   */
  @RequiredUIAccess
  void apply() throws ConfigurationException;

  /**
   * Load settings from other components to configurable.
   */
  @RequiredUIAccess
  default void reset() {
  }

  /**
   * Initializing method for configurable. Called one time, after configurable opened, and before #reset(). But #reset() can be called more than one time
   */
  @RequiredUIAccess
  default void initialize() {
  }

  /**
   * Disposes the Swing components used for displaying the configuration.
   */
  @RequiredUIAccess
  default void disposeUIResources() {
  }
}
