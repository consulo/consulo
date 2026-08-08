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
import com.vaadin.flow.component.dependency.NpmPackage;

@NpmPackage(value = "xterm-addon-fit", version = "0.7.0")
public interface ITerminalFit extends HasElement {

  default void fit() {
    getElement().executeJs("this.fit()");
  }

  default void setFitOnResize(boolean value) {
    getElement().setProperty("fitOnResize", value);
  }

  default boolean isFitOnResize() {
    return getElement().getProperty("fitOnResize", false);
  }
}
