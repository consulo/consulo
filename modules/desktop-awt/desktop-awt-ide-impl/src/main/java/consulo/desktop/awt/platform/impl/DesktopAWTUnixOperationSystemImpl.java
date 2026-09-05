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
package consulo.desktop.awt.platform.impl;

import consulo.platform.impl.UnixOperationSystemImpl;
import consulo.platform.os.UnixDisplayProtocol;
import consulo.util.lang.lazy.LazyValue;

import java.awt.*;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2026-09-05
 */
public class DesktopAWTUnixOperationSystemImpl extends UnixOperationSystemImpl {
    private static final String WAYLAND_TOOLKIT_CLASS_NAME = "sun.awt.wl.WLToolkit";

    private final LazyValue<UnixDisplayProtocol> myDisplayProtocol = LazyValue.atomicNotNull(() -> {
        String toolkitClassName;
        try {
            toolkitClassName = Toolkit.getDefaultToolkit().getClass().getName();
        }
        catch (Throwable e) {
            return super.displayProtocol();
        }

        return WAYLAND_TOOLKIT_CLASS_NAME.equals(toolkitClassName)
            ? UnixDisplayProtocol.WAYLAND
            : UnixDisplayProtocol.X11;
    });

    public DesktopAWTUnixOperationSystemImpl(Map<String, String> jvmProperties,
                                             Function<String, String> getEnvFunc,
                                             Supplier<Map<String, String>> getEnvsSup) {
        super(jvmProperties, getEnvFunc, getEnvsSup);
    }

    @Override
    public UnixDisplayProtocol displayProtocol() {
        return myDisplayProtocol.get();
    }
}
