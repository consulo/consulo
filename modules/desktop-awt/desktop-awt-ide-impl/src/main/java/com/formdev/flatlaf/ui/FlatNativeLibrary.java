/*
 * Copyright 2022 FormDev Software GmbH
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

package com.formdev.flatlaf.ui;

import com.formdev.flatlaf.FlatSystemProperties;
import com.formdev.flatlaf.util.LoggingFacade;
import com.formdev.flatlaf.util.NativeLibrary;
import consulo.component.util.NativeFileLoader;

/**
 * Helper class to load FlatLaf native library (.dll, .so or .dylib),
 * if available for current operating system and CPU architecture.
 *
 * @author Karl Tauber
 * @since 2.3
 */
class FlatNativeLibrary {
    private static boolean initialized;
    private static NativeLibrary nativeLibrary;

    private native static int getApiVersion();

    /**
     * Loads native library (if available) and returns whether loaded successfully.
     * Returns {@code false} if no native library is available.
     */
    static synchronized boolean isLoaded(int apiVersion) {
        initialize(apiVersion);
        return nativeLibrary != null && nativeLibrary.isLoaded();
    }

    private static void initialize(int apiVersion) {
        if (initialized) {
            return;
        }
        initialized = true;

        if (!FlatSystemProperties.getBoolean(FlatSystemProperties.USE_NATIVE_LIBRARY, true)) {
            return;
        }

        try {
            loadJAWT();

            NativeFileLoader.loadLibrary("flatlaf", System::load);

            FlatNativeLibrary.nativeLibrary = new NativeLibrary(true);
        }
        catch (Exception ignored) {
        }
    }
    
    private static void loadJAWT() {
        try {
            System.loadLibrary("jawt");
        }
        catch (UnsatisfiedLinkError ex) {
            // log error only if native library jawt.dll not already loaded
            String message = ex.getMessage();
            if (message == null || !message.contains("already loaded in another classloader")) {
                LoggingFacade.INSTANCE.logSevere(message, ex);
            }
        }
        catch (Exception ex) {
            LoggingFacade.INSTANCE.logSevere(ex.getMessage(), ex);
        }
    }
}
