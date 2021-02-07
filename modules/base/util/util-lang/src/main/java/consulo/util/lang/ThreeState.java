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

/*
 * @author max
 */
package consulo.util.lang;

import javax.annotation.Nonnull;

public enum ThreeState {
  YES,
  NO,
  UNSURE;

  public static ThreeState fromBoolean(boolean value) {
    return value ? YES : NO;
  }

  /**
   * Combine two different ThreeState values yielding UNSURE if values are different
   * and itself if values are the same.
   *
   * @param other other value to combine with this value
   * @return a result of combination of two ThreeState values
   */
  @Nonnull
  public ThreeState merge(ThreeState other) {
    return this == other ? this : UNSURE;
  }

  public boolean toBoolean() {
    if (this == UNSURE) {
      throw new IllegalStateException("Must be or YES, or NO");
    }
    return this == YES;
  }

  /**
   * @return {@code YES} if the given states contain {@code YES}, otherwise {@code UNSURE} if the given states contain {@code UNSURE}, otherwise {@code NO}
   */
  @Nonnull
  public static ThreeState mostPositive(@Nonnull Iterable<ThreeState> states) {
    ThreeState result = NO;
    for (ThreeState state : states) {
      switch (state) {
        case YES:
          return YES;
        case UNSURE:
          result = UNSURE;
      }
    }
    return result;
  }

  /**
   * @return {@code UNSURE} if {@code states} contains different values, the single value otherwise
   * @throws IllegalArgumentException if {@code states} is empty
   */
  @Nonnull
  public static ThreeState merge(@Nonnull Iterable<ThreeState> states) {
    ThreeState result = null;
    for (ThreeState state : states) {
      if (state == UNSURE) {
        return UNSURE;
      }
      if (result == null) {
        result = state;
      }
      else if (result != state) {
        return UNSURE;
      }
    }
    if (result == null) throw new IllegalArgumentException("Argument should not be empty");
    return result;
  }
}