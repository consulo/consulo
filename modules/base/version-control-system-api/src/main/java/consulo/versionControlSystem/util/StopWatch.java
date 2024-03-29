/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.versionControlSystem.util;

import consulo.logging.Logger;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class StopWatch {

  private static final Logger LOG = Logger.getInstance(StopWatch.class);

  private static final String[] UNIT_NAMES = new String[]{"s", "m", "h"};
  private static final long[] UNITS = new long[]{1, 60, 60 * 60};
  private static final String MSEC_FORMAT = "%03d";

  private final long myStartTime;
  @Nonnull
  private final String myOperation;
  @Nonnull
  private final Map<VirtualFile, Long> myDurationPerRoot;

  private StopWatch(@Nonnull String operation) {
    myOperation = operation;
    myStartTime = System.currentTimeMillis();
    myDurationPerRoot = new HashMap<>();
  }

  @Nonnull
  public static StopWatch start(@Nonnull String operation) {
    return new StopWatch(operation);
  }

  public void rootCompleted(@Nonnull VirtualFile root) {
    long totalDuration = System.currentTimeMillis() - myStartTime;
    long duration = totalDuration - sum(myDurationPerRoot.values());
    myDurationPerRoot.put(root, duration);
  }

  private static long sum(@Nonnull Collection<Long> durations) {
    long sum = 0;
    for (Long duration : durations) {
      sum += duration;
    }
    return sum;
  }

  public void report() {
    String message = myOperation + " took " + formatTime(System.currentTimeMillis() - myStartTime);
    if (myDurationPerRoot.size() > 1) {
      message += "\n" + StringUtil.join(myDurationPerRoot.entrySet(),
                                        entry -> "    " + entry.getKey().getName() + ": " + formatTime(entry.getValue()), "\n");
    }
    LOG.debug(message);
  }

  /**
   * 1h 1m 1.001s
   */
  @Nonnull
  public static String formatTime(long time) {
    if (time < 1000 * UNITS[0]) {
      return time + "ms";
    }
    String result = "";
    long remainder = time / 1000;
    long msec = time % 1000;
    for (int i = UNITS.length - 1; i >= 0; i--) {
      if (remainder < UNITS[i]) continue;

      long quotient = remainder / UNITS[i];
      remainder = remainder % UNITS[i];

      if (i == 0) {
        result += quotient + (msec == 0 ? "" : "." + String.format(MSEC_FORMAT, msec)) + UNIT_NAMES[i];
      }
      else {
        result += quotient + UNIT_NAMES[i] + " ";
        if (remainder == 0 && msec != 0) {
          result += "0." + String.format(MSEC_FORMAT, msec) + UNIT_NAMES[0];
        }
      }
    }

    return result;
  }
}
