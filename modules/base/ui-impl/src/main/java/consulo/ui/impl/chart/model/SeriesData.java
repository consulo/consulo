/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.ui.impl.chart.model;

import java.util.Objects;

/**
 * This class stores key-value ({@link #x} as a key) data.
 */
public final class SeriesData<T> {
    public final long x;
    public T value;

    public SeriesData(long x, T value) {
        this.x = x;
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SeriesData<?> that)) {
            return false;
        }
        return x == that.x && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return 31 * Long.hashCode(x) + Objects.hashCode(value);
    }

    @Override
    public String toString() {
        return "SeriesData(x=" + x + ", value=" + value + ")";
    }
}
