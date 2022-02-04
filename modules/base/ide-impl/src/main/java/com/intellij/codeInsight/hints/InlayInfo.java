/*
 * Copyright 2013-2017 consulo.io
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.codeInsight.hints;

import javax.annotation.Nonnull;

import java.util.Objects;

/**
 * from kotlin
 */
public class InlayInfo {
  private String myText;
  private int myOffset;

  public InlayInfo(@Nonnull String text, int offset) {
    myText = text;
    myOffset = offset;
  }

  @Nonnull
  public String getText() {
    return myText;
  }

  public int getOffset() {
    return myOffset;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("InlayInfo{");
    sb.append("myText='").append(myText).append('\'');
    sb.append(", myOffset=").append(myOffset);
    sb.append('}');
    return sb.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof InlayInfo)) return false;
    InlayInfo inlayInfo = (InlayInfo)o;
    return myOffset == inlayInfo.myOffset && Objects.equals(myText, inlayInfo.myText);
  }

  @Override
  public int hashCode() {
    return Objects.hash(myText, myOffset);
  }
}
