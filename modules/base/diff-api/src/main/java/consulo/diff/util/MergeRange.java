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
package consulo.diff.util;

public record MergeRange(int start1, int end1, int start2, int end2, int start3, int end3) {
  @Override
  public String toString() {
    return "[" + start1 + ", " + end1 + ") - [" + start2 + ", " + end2 + ") - [" + start3 + ", " + end3 + ")";
  }

  public boolean isEmpty() {
    return start1 == end1 && start2 == end2 && start3 == end3;
  }
}
