/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package consulo.diff;

import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;

import java.util.Iterator;

public class DiffNavigationContext {
  private final String myTargetString;
  private final Iterable<String> myPreviousLinesIterable;

  public DiffNavigationContext(@Nonnull Iterable<String> previousLinesIterable, @Nonnull String targetString) {
    myPreviousLinesIterable = previousLinesIterable;
    myTargetString = targetString;
  }

  public Iterable<String> getPreviousLinesIterable() {
    return myPreviousLinesIterable;
  }

  public String getTargetString() {
    return myTargetString;
  }


  public int contextMatchCheck(@Nonnull Iterator<Pair<Integer, CharSequence>> changedLinesIterator) {
    // we ignore spaces.. at least at start/end, since some version controls could ignore their changes when doing annotate
    Iterator<? extends CharSequence> iterator = getPreviousLinesIterable().iterator();

    if (iterator.hasNext()) {
      CharSequence contextLine = iterator.next();

      while (changedLinesIterator.hasNext()) {
        Pair<Integer, ? extends CharSequence> pair = changedLinesIterator.next();
        if (StringUtil.equalsTrimWhitespaces(pair.getSecond(), contextLine)) {
          if (!iterator.hasNext()) break;
          contextLine = iterator.next();
        }
      }
    }
    if (iterator.hasNext()) return -1;
    if (!changedLinesIterator.hasNext()) return -1;

    while (changedLinesIterator.hasNext()) {
      Pair<Integer, ? extends CharSequence> pair = changedLinesIterator.next();
      if (StringUtil.equalsTrimWhitespaces(pair.getSecond(), getTargetString())) {
        return pair.getFirst();
      }
    }

    return -1;
  }
}
