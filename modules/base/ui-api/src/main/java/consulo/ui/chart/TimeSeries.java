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

import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;

import java.time.Instant;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
public interface TimeSeries {
    LocalizeValue getName();

    TimeSeriesKind getKind();

    @RequiredUIAccess
    void add(Instant time, double value);

    @RequiredUIAccess
    void setColor(ColorValue color);

    @RequiredUIAccess
    void clear();
}
