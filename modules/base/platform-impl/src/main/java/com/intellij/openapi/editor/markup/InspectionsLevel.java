/*
 * Copyright 2013-2020 consulo.io
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
package com.intellij.openapi.editor.markup;

import com.intellij.openapi.editor.EditorBundle;
import org.jetbrains.annotations.PropertyKey;

/**
 * @author VISTALL
 * @since 2020-06-19
 */
public enum InspectionsLevel {
  NONE("iw.level.none"),
  ERRORS("iw.level.errors"),
  ALL("iw.level.all");

  private final String myKey;

  InspectionsLevel(@PropertyKey(resourceBundle = "messages.EditorBundle") String key) {
    myKey = key;
  }

  @Override
  public String toString() {
    return EditorBundle.message(myKey);
  }
}
