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
package consulo.ui.chart;

import consulo.disposer.Disposable;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.internal.UIInternal;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
public interface TimeAxis {
    static TimeAxis create() {
        return create(Instant.now());
    }

    static TimeAxis create(Instant dataStart) {
        return UIInternal.get()._Chart_timeAxis(dataStart);
    }

    @Nullable
    TimeRange getVisible();

    @RequiredUIAccess
    void setVisible(TimeRange range);

    @RequiredUIAccess
    void setFollowLatest(Duration window);

    boolean isFollowingLatest();

    @Nullable
    TimeRange getSelection();

    @RequiredUIAccess
    void setSelection(@Nullable TimeRange selection);

    Disposable addVisibleListener(Consumer<TimeRange> listener);

    Disposable addSelectionListener(Consumer<@Nullable TimeRange> listener);
}
