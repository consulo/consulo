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
package consulo.platform.impl;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2025-02-01
 */
public class LocalMacOperatingSystemImpl extends MacOperatingSystemImpl {
    private boolean myEnabledTopMenu;

    public LocalMacOperatingSystemImpl(Map<String, String> jvmProperties,
                                       Function<String, String> getEnvFunc,
                                       Supplier<Map<String, String>> getEnvsSup) {
        super(jvmProperties, getEnvFunc, getEnvsSup);
        myEnabledTopMenu = "true".equals(jvmProperties.get("apple.laf.useScreenMenuBar"));
    }

    @Override
    public boolean isEnabledTopMenu() {
        return myEnabledTopMenu;
    }
}
