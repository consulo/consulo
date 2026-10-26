/*
 * Copyright 2013-2025 consulo.io
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
package consulo.ui;

/**
 * @author VISTALL
 * @since 2025-12-20
 */
public enum ProgressBarStyle implements ComponentStyle {
    SPINNER,
    TRANSPARENT_BACKGROUND,

    /**
     * Tones of the filled part - how good the value is, such as how full a disk is. They exclude each other; adding one
     * replaces the last.
     */
    SUCCESS,
    WARNING,
    ERROR,

    /** Thicker bars than the default line. They exclude each other; adding one replaces the last. */
    HEIGHT_MEDIUM,
    HEIGHT_LARGE;

    public boolean isTone() {
        return this == SUCCESS || this == WARNING || this == ERROR;
    }

    public boolean isHeight() {
        return this == HEIGHT_MEDIUM || this == HEIGHT_LARGE;
    }
}
