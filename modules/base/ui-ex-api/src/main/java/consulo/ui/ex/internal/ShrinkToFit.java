/*
 * Copyright 2013-2026 consulo.io
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
package consulo.ui.ex.internal;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
public final class ShrinkToFit {
    private ShrinkToFit() {
    }

    public static int[] widths(int[] preferred, int[] minimum, long deficit) {
        int count = preferred.length;
        int[] floor = new int[count];
        int widest = 0;
        for (int i = 0; i < count; i++) {
            floor[i] = Math.min(preferred[i], minimum[i]);
            widest = Math.max(widest, preferred[i]);
        }

        int low = 0;
        int high = widest;
        while (low < high) {
            int level = (low + high + 1) >>> 1;
            if (cutAtLevel(preferred, floor, level) >= deficit) {
                low = level;
            }
            else {
                high = level - 1;
            }
        }

        int level = low;
        long excess = Math.max(0, cutAtLevel(preferred, floor, level) - deficit);
        int[] widths = new int[count];
        for (int i = 0; i < count; i++) {
            int fitted = widthAtLevel(preferred[i], floor[i], level);
            if (excess > 0 && fitted == level && fitted < preferred[i]) {
                fitted++;
                excess--;
            }
            widths[i] = fitted;
        }
        return widths;
    }

    private static long cutAtLevel(int[] preferred, int[] floor, int level) {
        long cut = 0;
        for (int i = 0; i < preferred.length; i++) {
            cut += preferred[i] - widthAtLevel(preferred[i], floor[i], level);
        }
        return cut;
    }

    private static int widthAtLevel(int preferred, int floor, int level) {
        return Math.max(floor, Math.min(preferred, level));
    }
}
