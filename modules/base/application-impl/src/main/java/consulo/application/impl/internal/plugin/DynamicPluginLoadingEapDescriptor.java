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
package consulo.application.impl.internal.plugin;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.eap.EarlyAccessProgramDescriptor;
import consulo.localize.LocalizeValue;

/**
 * @author VISTALL
 * @since 2026-03-08
 */
@ExtensionImpl
public class DynamicPluginLoadingEapDescriptor extends EarlyAccessProgramDescriptor {
    @Override
    public LocalizeValue getName() {
        return LocalizeValue.localizeTODO("Install plugins without restart");
    }

    @Override
    public LocalizeValue getDescription() {
        return LocalizeValue.localizeTODO("Load plugins installed from the plugin manager right away, when they support it");
    }
}
