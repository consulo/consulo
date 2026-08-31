/*
 * Copyright 2013-2026 consulo.io
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
package com.flowingcode.vaadin.addons.xterm;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;

/**
 * Terminal talking to a pty. The line editing, history and prompt of {@link XTerm} belong to a console the
 * server draws, and get in the way of a process which draws its own screen.
 *
 * @author VISTALL
 * @since 2026-08-08
 */
@SuppressWarnings("serial")
@Tag("consulo-pty-term")
@JsModule("Frontend/fc-xterm/consulo-pty-term.ts")
public class ConsuloPtyTerm extends XTermBase implements ITerminalFit {
}
