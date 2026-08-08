/*-
 * #%L
 * XTerm Console Addon
 * %%
 * Copyright (C) 2020 - 2023 Flowing Code
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package com.flowingcode.vaadin.addons.xterm;

import com.vaadin.flow.component.HasElement;

/**
 * Add selection support to XTerm using arrow keys.
 */
public interface ITerminalSelection extends HasElement {

  /** Sets the command line prompt. */
  default void setKeyboardSelectionEnabled(boolean enabled) {
    getElement().setProperty("keyboardSelectionEnabled", enabled);
  }

  /** Returns the command line prompt. */
  default boolean getKeyboardSelectionEnabled() {
    // the feature is enabled by default
    // getProperty defaults to false in case the mixin isn't applied
    return getElement().getProperty("keyboardSelectionEnabled", false);
  }

}
