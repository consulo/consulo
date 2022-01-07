/*
 * Copyright 2013-2021 consulo.io
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
package com.intellij.openapi.editor.actions;

import com.intellij.util.xmlb.annotations.Attribute;

import java.util.Objects;

// from kotlin
public final class CaretStop {
  public static final CaretStop NONE = new CaretStop(false, false);
  public static final CaretStop START = new CaretStop(true, false);
  public static final CaretStop END = new CaretStop(false, true);
  public static final CaretStop BOTH = new CaretStop(true, true);

  @Attribute("start")
  private final boolean atStart;
  @Attribute("end")
  private final boolean atEnd;

  public CaretStop(boolean atStart, boolean atEnd) {
    this.atStart = atStart;
    this.atEnd = atEnd;
  }

  public boolean isAtStart() {
    return atStart;
  }

  public boolean isAtEnd() {
    return atEnd;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    CaretStop caretStop = (CaretStop)o;
    return atStart == caretStop.atStart && atEnd == caretStop.atEnd;
  }

  @Override
  public int hashCode() {
    return Objects.hash(atStart, atEnd);
  }

  @Override
  public String toString() {
    return "CaretStop{" + "atStart=" + atStart + ", atEnd=" + atEnd + '}';
  }
}
