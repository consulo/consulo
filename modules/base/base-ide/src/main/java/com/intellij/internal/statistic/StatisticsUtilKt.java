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
package com.intellij.internal.statistic;

import com.intellij.internal.statistic.beans.UsageDescriptor;
import com.intellij.util.containers.ContainerUtil;

import java.util.Arrays;
import java.util.List;

/**
 * @author VISTALL
 * @since 10-Sep-16
 * <p>
 * port com/intellij/internal/statistic/StatisticsUtil.kt
 */
public class StatisticsUtilKt {
  /**
   * Constructs a proper UsageDescriptor for a boolean value,
   * by adding "enabled" or "disabled" suffix to the given key, depending on the value.
   */
  public static UsageDescriptor getBooleanUsage(String key, boolean value) {
    key += value ? ".enabled" : ".disabled";
    return new UsageDescriptor(key, 1);
  }

  /**
   * Constructs a proper UsageDescriptor for a counting value.
   * If one needs to know a number of some items in the project, there is no direct way to report usages per-project.
   * Therefore this workaround: create several keys representing interesting ranges, and report that key which correspond to the range
   * which the given value belongs to.
   * <p>
   * For example, to report a number of commits in Git repository, you can call this method like that:
   * ```
   * val usageDescriptor = getCountingUsage("git.commit.count", listOf(0, 1, 100, 10000, 100000), realCommitCount)
   * ```
   * and if there are e.g. 50000 commits in the repository, one usage of the following key will be reported: `git.commit.count.10K+`.
   * <p>
   * NB:
   * (1) the list of steps must be sorted ascendingly; If it is not, the result is undefined.
   * (2) the value should lay somewhere inside steps ranges. If it is below the first step, the following usage will be reported:
   * `git.commit.count.<1`.
   *
   * @key The key prefix which will be appended with "." and range code.
   * @steps Limits of the ranges. Each value represents the start of the next range. The list must be sorted ascendingly.
   * @value Value to be checked among the given ranges.
   */
  public static UsageDescriptor getCountingUsage(String key, int value, List<Integer> steps) {
    if (steps.isEmpty()) return new UsageDescriptor(key + "." + value, 1);

    if (value < steps.get(0)) {
      return new UsageDescriptor(key + ".<" + steps.get(0), 1);
    }

    int index = Arrays.binarySearch(steps.toArray(), value);
    int stepIndex;

    if (index == steps.size()) {
      stepIndex = ContainerUtil.getLastItem(steps);
    }
    else if (index >= 0) {
      stepIndex = index;
    }
    else {
      stepIndex = -index - 2;
    }

    int step = steps.get(stepIndex);

    boolean addPlus = stepIndex == steps.size() - 1 || steps.get(stepIndex + 1) != step + 1;

    String maybePlus = addPlus ? "+" : "";
    return new UsageDescriptor(key + "." + humanize(step) + maybePlus, 1);
  }

  private static final int kilo = 1000;
  private static final int mega = kilo * kilo;

  private static String humanize(int number) {
    if (number == 0) return "0";
    int m = number / mega;
    int k = (number % mega) / kilo;
    int r = (number % kilo);
    String ms = m > 0 ? (m + "M") : "";
    String ks = k > 0 ? (k + "K") : "";
    String rs = r > 0 ? String.valueOf(r) : "";
    return ms + ks + rs;
  }

}
