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
package consulo.it.internal;

import consulo.application.internal.ProgressIndicatorBase;
import consulo.application.internal.UnsafeProgressIndicator;

/**
 * Headless stand-in for the UI {@code BackgroundableProcessIndicator}: a plain
 * {@link ProgressIndicatorBase} that also carries the {@link UnsafeProgressIndicator} marker which
 * {@code DumbServiceImpl.runBackgroundProcess} casts to when running background index updates.
 *
 * @author VISTALL
 */
public class HeadlessBackgroundableProcessIndicator extends ProgressIndicatorBase implements UnsafeProgressIndicator {
    private volatile boolean myUnsafeIndicator;

    @Override
    public boolean isUnsafeIndicator() {
        return myUnsafeIndicator;
    }

    @Override
    public void markAsUnsafeIndicator() {
        myUnsafeIndicator = true;
    }
}
