/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.diff.impl.internal.fragment;

import consulo.diff.util.Side;
import consulo.document.util.TextRange;

public class HighlightRange {
  
  private final TextRange myBase;
  
  private final TextRange myChanged;
  
  private final Side mySide;

  public HighlightRange(Side side, TextRange base, TextRange changed) {
    mySide = side;
    myBase = base;
    myChanged = changed;
  }

  
  public Side getSide() {
    return mySide;
  }

  
  public TextRange getBase() {
    return myBase;
  }

  
  public TextRange getChanged() {
    return myChanged;
  }
}
