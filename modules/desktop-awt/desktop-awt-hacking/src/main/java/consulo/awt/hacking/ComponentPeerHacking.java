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
package consulo.awt.hacking;

import consulo.logging.Logger;
import jakarta.annotation.Nullable;
import sun.awt.AWTAccessor;

import java.awt.*;
import java.awt.peer.ComponentPeer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author VISTALL
 * @since 2025-06-08
 */
public class ComponentPeerHacking {
    private static final Logger LOG = Logger.getInstance(ComponentPeerHacking.class);

    public static long getNSViewPtr(@Nullable Window window) {
        if (window == null) {
            return 0;
        }

        long nsViewPtr = 0;
        final ComponentPeer peer = AWTAccessor.getComponentAccessor().getPeer(window);

        // sun.lwawt.* isn't available outside of java.desktop => use reflection
        if (peer != null && peer.getClass().getName().equals("sun.lwawt.LWWindowPeer")) {
            final Method methodGetPlatformWindow;
            try {
                methodGetPlatformWindow = peer.getClass().getMethod("getPlatformWindow");
                Object platformWindow = methodGetPlatformWindow.invoke(peer);
                if (platformWindow != null && platformWindow.getClass().getName().equals("sun.lwawt.macosx.CPlatformWindow")) {
                    final Method methodGetContentView = platformWindow.getClass().getMethod("getContentView");
                    Object contentView = methodGetContentView.invoke(platformWindow);
                    final Method methodGetAWTView = contentView.getClass().getMethod("getAWTView");
                    nsViewPtr = (long) methodGetAWTView.invoke(contentView);
                }
                else {
                    LOG.warn("platformWindow of frame peer isn't instance of sun.lwawt.macosx.CPlatformWindow, class of platformWindow: " +
                        (platformWindow != null ? platformWindow.getClass() : "null"));
                }
            }
            catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                LOG.warn(e);
            }
        }
        else {
            if (peer == null) {
                LOG.warn("frame peer is null, window: " + window);
            }
            else {
                LOG.warn("frame peer isn't instance of sun.lwawt.LWWindowPeer, class of peer: " + peer.getClass());
            }
        }

        return nsViewPtr;
    }
}
