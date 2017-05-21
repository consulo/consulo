/*
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellij.rt.coverage.util;

import gnu.trove.TIntObjectHashMap;

import com.intellij.rt.coverage.data.LineData;

/*
 * @author anna
 * @since 26-Feb-2010
 */
public class LinesUtil {
  public static LineData[] calcLineArray(final int maxLineNumber, final TIntObjectHashMap lines) {
    final LineData[] linesArray = new LineData[maxLineNumber + 1];
    for(int line = 1; line <= maxLineNumber; line++) {
      final LineData lineData = (LineData) lines.get(line);
      if (lineData != null) {
        lineData.fillArrays();
      }
      linesArray[line] = lineData;
    }
    return linesArray;
  }
}