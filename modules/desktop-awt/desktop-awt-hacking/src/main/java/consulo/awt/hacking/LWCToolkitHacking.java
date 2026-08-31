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
package consulo.awt.hacking;

import consulo.logging.Logger;

import java.awt.*;
import java.lang.reflect.Method;

/**
 * Access to {@code sun.lwawt.macosx.LWCToolkit}, which runs a runnable on the AppKit thread. macOS only and not part
 * of the public API, so it is reached by reflection (the class is absent on other platforms). {@code sun.lwawt.macosx}
 * is opened to this module by the boot layer.
 */
public class LWCToolkitHacking {
    private static final Logger LOG = Logger.getInstance(LWCToolkitHacking.class);

    private static boolean ourInitialized;
    private static Method ourInvokeAndWait;
    private static Method ourInvokeLater;

    private static synchronized void init() {
        if (ourInitialized) {
            return;
        }
        ourInitialized = true;
        try {
            Class<?> clazz = Class.forName("sun.lwawt.macosx.LWCToolkit");
            ourInvokeAndWait = clazz.getMethod("invokeAndWait", Runnable.class, Component.class, boolean.class, int.class);
            ourInvokeLater = clazz.getMethod("invokeLater", Runnable.class, Component.class);
        }
        catch (Throwable e) {
            LOG.warn("can't find sun.lwawt.macosx.LWCToolkit", e);
        }
    }

    /**
     * Runs {@code runnable} through {@code LWCToolkit}.
     *
     * @param wait whether to wait for completion ({@code invokeAndWait}) or not ({@code invokeLater})
     * @return {@code true} if the call was dispatched
     */
    public static boolean invoke(Runnable runnable, Component invoker, boolean wait) {
        init();
        try {
            if (wait) {
                if (ourInvokeAndWait == null) {
                    return false;
                }
                ourInvokeAndWait.invoke(null, runnable, invoker, true, -1);
            }
            else {
                if (ourInvokeLater == null) {
                    return false;
                }
                ourInvokeLater.invoke(null, runnable, invoker);
            }
            return true;
        }
        catch (Throwable e) {
            LOG.warn("LWCToolkit invoke failed: " + e);
            return false;
        }
    }
}
