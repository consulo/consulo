/*
 * Copyright 2021 FormDev Software GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.formdev.flatlaf.util;

/**
 * Helper class to load native library (.dll, .so or .dylib) stored in Jar.
 * <p>
 * Copies native library to users temporary folder before loading it.
 *
 * @author Karl Tauber
 * @since 1.1
 */
public class NativeLibrary {
    private final boolean loaded;

    public NativeLibrary(boolean loaded) {
        this.loaded = loaded;
    }

    /**
     * Returns whether the native library is loaded.
     * <p>
     * Returns {@code false} if not supported on current platform as specified in constructor
     * or if loading failed.
     */
    public boolean isLoaded() {
        return loaded;
    }
}
