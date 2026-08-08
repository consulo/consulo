/*-
 * #%L
 * XTerm Console Addon
 * %%
 * Copyright (C) 2020 - 2026 Flowing Code
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

import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.JacksonCodec;
import com.vaadin.flow.internal.JacksonUtils;
import java.io.Serializable;
import tools.jackson.databind.node.ArrayNode;

/**
 * Represents an abstract base class for server-side terminal add-ons that have a corresponding
 * client-side (JavaScript) component or require interaction with the client-side terminal
 * environment. It extends {@link TerminalAddon} and specializes its use for client-aware
 * operations.
 *
 * @author Javier Godoy / Flowing Code S.A.
 */
@SuppressWarnings("serial")
public abstract class ClientTerminalAddon extends TerminalAddon {

  private final XTermBase xterm;

  /**
   * Constructs a new {@code ClientTerminalAddon} and associates it with the specified
   * {@link XTermBase} instance.
   * <p>
   * This constructor ensures the add-on is registered with the terminal and verifies that the
   * add-on's name, as returned by {@link #getName()}, is not {@code null}. A non-null name is
   * required for client-side add-ons to be uniquely identified and targeted for JavaScript
   * execution.
   * </p>
   *
   * @param xterm the {@link XTermBase} instance this add-on will be attached to. Must not be
   *        {@code null}.
   * @throws NullPointerException if {@code xterm} is {@code null}
   * @throws IllegalStateException if {@link #getName()} returns {@code null} immediately after
   *         superclass construction. This check relies on {@code getName()} being a static value.
   */
  protected ClientTerminalAddon(XTermBase xterm) {
    super(xterm);
    this.xterm = xterm;
    if (getName() == null) {
      throw new IllegalStateException("getName() must return a non-null value");
    }
  }

  /**
   * The xterm instance that this add-on is associated with.
   */
  protected XTermBase getXterm() {
    return xterm;
  }

  /**
   * Retrieves the unique name of this client-side add-on.
   * <p>
   * This name is used by {@link #executeJs(String, Serializable...)} to target the corresponding
   * JavaScript object on the client (i.e., {@code this.addons[name]} within the client-side
   * terminal's scope). The name effectively acts as a key in a client-side add-ons collection
   * managed by the terminal.
   * </p>
   *
   * @return the unique, non-null string identifier for the client-side counterpart of this add-on.
   *         Subclasses must implement this to provide a name for add-on-specific JavaScript
   *         execution.
   */
  protected abstract String getName();

  /**
   * Executes a JavaScript {@code expression} in the context of this add-on, with the specified
   * {@code parameters}.
   *
   * @see #getName()
   * @see Element#executeJs(String, Serializable...)
   */
  protected final void executeJs(String expression, Serializable... parameters) {
    String name = getName();

    ArrayNode args = JacksonUtils.createArrayNode();
    for (Serializable parameter : parameters) {
      args.add(JacksonCodec.encodeWithTypeInfo(parameter));
    }

    expression = expression.replaceAll("\\$(\\d+)", "\\$1[$1]");
    xterm.executeJs("(function(){" + expression + "}).apply(this.addons[$0],$1);", name, args);
  }

}
