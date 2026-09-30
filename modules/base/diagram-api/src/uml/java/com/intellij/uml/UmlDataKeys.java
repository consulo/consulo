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

package com.intellij.uml;

import com.intellij.openapi.actionSystem.DataKey;
import com.intellij.openapi.graph.builder.GraphBuilder;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.util.Key;

/**
 * @author Konstantin Bulenkov
 */

public class UmlDataKeys {
  public static final DataKey<GraphBuilder<UmlNode, UmlEdge>> BUILDER = DataKey.create("UML_BUILDER");
  public static final Key<JBPopup> UML_POPUP = Key.create("UML_POPUP");

  private UmlDataKeys() {
  }
}

