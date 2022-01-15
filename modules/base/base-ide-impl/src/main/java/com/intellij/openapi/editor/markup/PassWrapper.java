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

import java.util.Objects;

/**
 * Light wrapper for <code>ProgressableTextEditorHighlightingPass</code> with only essential UI data.
 * <p>
 * from kotlin
 */
public final class PassWrapper {
  private final String myPresentableName;
  private final double myProgress;
  private final boolean myFinished;

  public PassWrapper(String presentableName, double progress, boolean finished) {
    myPresentableName = presentableName;
    myProgress = progress;
    myFinished = finished;
  }

  public int toPercent() {
    int percent = (int)Math.round(myProgress * 100);

    return percent == 100 && !myFinished ? 99 : percent;
  }

  public String getPresentableName() {
    return myPresentableName;
  }

  public double getProgress() {
    return myProgress;
  }

  public boolean isFinished() {
    return myFinished;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    PassWrapper that = (PassWrapper)o;
    return Double.compare(that.myProgress, myProgress) == 0 && myFinished == that.myFinished && Objects.equals(myPresentableName, that.myPresentableName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(myPresentableName, myProgress, myFinished);
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("PassWrapper{");
    sb.append("myPresentableName='").append(myPresentableName).append('\'');
    sb.append(", myProgress=").append(myProgress);
    sb.append(", myFinished=").append(myFinished);
    sb.append('}');
    return sb.toString();
  }
}
