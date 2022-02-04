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

import com.intellij.util.xmlb.annotations.OptionTag;

import java.util.Objects;

// from kotlin
public final class CaretStopPolicy {
  public static final CaretStopPolicy NONE = new CaretStopPolicy(CaretStop.NONE, CaretStop.NONE);
  public static final CaretStopPolicy WORD_START = new CaretStopPolicy(CaretStop.START, CaretStop.BOTH);
  public static final CaretStopPolicy WORD_END = new CaretStopPolicy(CaretStop.END, CaretStop.BOTH);
  public static final CaretStopPolicy BOTH = new CaretStopPolicy(CaretStop.BOTH, CaretStop.BOTH);

  @OptionTag("WORD")
  private final CaretStop wordStop;
  @OptionTag("LINE")
  private final CaretStop lineStop;

  public CaretStopPolicy(CaretStop wordStop, CaretStop lineStop) {
    this.wordStop = wordStop;
    this.lineStop = lineStop;
  }

  public CaretStop getWordStop() {
    return wordStop;
  }

  public CaretStop getLineStop() {
    return lineStop;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    CaretStopPolicy that = (CaretStopPolicy)o;
    return Objects.equals(wordStop, that.wordStop) && Objects.equals(lineStop, that.lineStop);
  }

  @Override
  public int hashCode() {
    return Objects.hash(wordStop, lineStop);
  }

  @Override
  public String toString() {
    return "CaretStopPolicy{" + "wordStop=" + wordStop + ", lineStop=" + lineStop + '}';
  }
}
