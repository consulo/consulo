/*-
 * #%L
 * XTerm Console Addon
 * %%
 * Copyright (C) 2020 - 2025 Flowing Code
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

import java.io.Serializable;
import java.util.Objects;

/**
 * Represents an abstract base class for server-side add-ons designed to extend or modify the
 * functionality of an {@link XTermBase} terminal instance.
 * <p>
 * Concrete add-on implementations should subclass this class to provide specific features. Each
 * add-on is tightly coupled with a specific {@code XTermBase} instance, allowing it to interact
 * with and enhance that terminal.
 * </p>
 *
 * @author Javier Godoy / Flowing Code S.A.
 */
@SuppressWarnings("serial")
public abstract class TerminalAddon implements Serializable {

  /**
   * Constructs a new {@code TerminalAddon} and associates it with the provided {@link XTermBase}
   * instance.
   *
   * @param xterm the {@code XTermBase} instance to which this add-on will be attached
   * @throws NullPointerException if the provided {@code xterm} is {@code null}
   */
  protected TerminalAddon(XTermBase xterm) {
    Objects.requireNonNull(xterm);
    xterm.registerServerSideAddon(this);
  }

}
