/*
 * Copyright 2013-2016 consulo.io
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
package consulo.web.gwt.shared.transport;

import consulo.annotation.DeprecationInfo;

import java.io.Serializable;

/**
 * @author VISTALL
 * @since 17-May-16
 */
@Deprecated
@DeprecationInfo("This is part of research 'consulo as web app'. Code was written in hacky style. Must be dropped, or replaced by Consulo UI API")
public class GwtTextRange implements Serializable {
  private int myStartOffset;
  private int myEndOffset;

  public GwtTextRange(int startOffset, int endOffset) {
    myStartOffset = startOffset;
    myEndOffset = endOffset;
  }

  public GwtTextRange() {
  }

  public int getStartOffset() {
    return myStartOffset;
  }

  public int getEndOffset() {
    return myEndOffset;
  }

  public boolean containsRange(GwtTextRange textRange) {
    return containsRange(textRange.myStartOffset, textRange.myEndOffset);
  }

  public boolean containsRange(int startOffset, int endOffset) {
    return getStartOffset() <= startOffset && getEndOffset() >= endOffset;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    GwtTextRange textRange = (GwtTextRange)o;

    if (myStartOffset != textRange.myStartOffset) return false;
    if (myEndOffset != textRange.myEndOffset) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = myStartOffset;
    result = 31 * result + myEndOffset;
    return result;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("GwtTextRange{");
    sb.append("myStartOffset=").append(myStartOffset);
    sb.append(", myEndOffset=").append(myEndOffset);
    sb.append('}');
    return sb.toString();
  }
}
