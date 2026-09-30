/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.intellij.uml.providers;

import com.intellij.openapi.actionSystem.CustomShortcutSet;
import com.intellij.openapi.actionSystem.ShortcutSet;
import static com.intellij.openapi.util.SystemInfo.isMac;
import com.intellij.uml.UmlElementsProviderBase;

import javax.swing.*;
import static java.awt.event.InputEvent.*;
import static java.awt.event.KeyEvent.VK_P;

/**
 * @author Konstantin Bulenkov
 */
public abstract class SupersProvider<T> extends UmlElementsProviderBase<T> {
  public ShortcutSet getShortcutSet() {
    return new CustomShortcutSet(KeyStroke.getKeyStroke(VK_P, (isMac ? META_MASK : CTRL_MASK) | ALT_MASK));
  }

  public String getName() {
    return "Show Parents";
  }
}
