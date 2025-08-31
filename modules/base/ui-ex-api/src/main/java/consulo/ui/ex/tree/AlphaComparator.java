/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.ui.ex.tree;

import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;

import java.util.Comparator;

public class AlphaComparator implements Comparator<NodeDescriptor>{
  public static final AlphaComparator INSTANCE = new AlphaComparator();

  protected AlphaComparator() {
  }

  @Override
  public int compare(NodeDescriptor nodeDescriptor1, NodeDescriptor nodeDescriptor2) {
    int weight1 = nodeDescriptor1.getWeight();
    int weight2 = nodeDescriptor2.getWeight();
    if (weight1 != weight2) {
      return weight1 - weight2;
    }
    String s1 = nodeDescriptor1.toString();
    String s2 = nodeDescriptor2.toString();
    if (s1 == null) return s2 == null ? 0 : -1;
    if (s2 == null) return +1;

    //for super natural comparison (IDEA-80435)
    Pair<String,String> normalized = normalize(s1, s2);
    return StringUtil.naturalCompare(normalized.first, normalized.second);
  }

  private static Pair<String, String> normalize(String s1, String s2) {
    int minLen = Math.min(s1.length(), s2.length());
    StringBuilder sb1 = new StringBuilder(s1);
    StringBuilder sb2 = new StringBuilder(s2);
    for (int i = 0; i < minLen; i++) {
      char ch1 = s1.charAt(i);
      char ch2 = sb2.charAt(i);
      if (ch1 == ch2 && ch1 == '-') {
        sb1.setCharAt(i, '_');
        sb2.setCharAt(i, '_');
      } else if (ch1 == '-' && ch2 != '_') {
        sb1.setCharAt(i, '_');
      } else if (ch2 == '-' && ch1 != '_') {
        sb2.setCharAt(i, '_');
      }
    }

    s1 = sb1.toString();
    s2 = sb2.toString();
    return Pair.create(s1, s2);
  }
}
