/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.ui.ex.awt.tree;

import consulo.ui.ex.tree.NodeDescriptor;
import consulo.util.lang.StringUtil;

import java.util.Comparator;

public class WeightBasedComparator implements Comparator<NodeDescriptor> {

  public static final int UNDEFINED_WEIGHT = Integer.MIN_VALUE;

  private final boolean myCompareToString;

  public static final WeightBasedComparator INSTANCE = new WeightBasedComparator();
  public static final WeightBasedComparator FULL_INSTANCE = new WeightBasedComparator(true) {
    @Override
    protected int compareWeights(int w1, int w2) {
      return w1 - w2;
    }
  };

  public WeightBasedComparator() {
    this(false);
  }

  public WeightBasedComparator(boolean compareToString) {
    myCompareToString = compareToString;
  }

  @Override
  public int compare(NodeDescriptor o1, NodeDescriptor o2) {
    int w1 = getWeight(o1);
    int w2 = getWeight(o2);
    if (myCompareToString && w1 == w2) {
      return compareToString(o1, o2);
    }

    int weights = compareWeights(w1, w2);
    return weights != 0 ? weights : o1.getIndex() - o2.getIndex();
  }

  protected int getWeight(NodeDescriptor o1) {
    return o1.getWeight();
  }

  protected int compareWeights(int w1, int w2) {
    return w2 - w1;
  }

  protected static int compareToString(NodeDescriptor first, NodeDescriptor second) {
    return StringUtil.compare(first.toString(), second.toString(), true);
  }
}
