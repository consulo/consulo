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

import consulo.annotations.RequiredDispatchThread;
import consulo.ui.Component;
import consulo.ui.RequiredUIAccess;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Component which provides a configuration user interface.
 *
 * @see consulo.options.SimpleConfigurable
 * @author lesya
 */
public interface UnnamedConfigurable {
  /**
   * Returns the user interface component for editing the configuration.
   *
   * @return the component instance.
   */
  @Nullable
  @RequiredDispatchThread
  default JComponent createComponent() {
    return null;
  }

  @Nullable
  @RequiredUIAccess
  default Component createUIComponent() {
    return null;
  }

  /**
   * Checks if the settings in the user interface component were modified by the user and
   * need to be saved.
   *
   * @return true if the settings were modified, false otherwise.
   */
  @RequiredDispatchThread
  boolean isModified();

  /**
   * Store the settings from configurable to other components.
   */
  @RequiredDispatchThread
  void apply() throws ConfigurationException;

  /**
   * Load settings from other components to configurable.
   */
  @RequiredDispatchThread
  void reset();

  /**
   * Disposes the Swing components used for displaying the configuration.
   */
  @RequiredDispatchThread
  default void disposeUIResources() {
  }
}
