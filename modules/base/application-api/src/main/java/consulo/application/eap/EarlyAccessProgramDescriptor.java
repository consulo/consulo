/*
 * Copyright 2013-2016 consulo.io
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
package consulo.application.eap;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 17:09/15.10.13
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class EarlyAccessProgramDescriptor {
    public static final ExtensionPointName<EarlyAccessProgramDescriptor> EP_NAME =
        ExtensionPointName.create(EarlyAccessProgramDescriptor.class);

    @Nonnull
    public abstract String getName();

    public boolean getDefaultState() {
        return false;
    }

    public boolean isAvailable() {
        return true;
    }

    public boolean isRestartRequired() {
        return false;
    }

    @Nullable
    public String getDescription() {
        return null;
    }
}
