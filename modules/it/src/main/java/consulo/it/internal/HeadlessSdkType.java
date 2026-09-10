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
package consulo.it.internal;

import consulo.annotation.component.ExtensionImpl;
import consulo.content.bundle.SdkType;
import consulo.localize.LocalizeValue;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

/**
 * An SDK type with no home layout of its own, so that a test can build an SDK out of any directory.
 *
 * @author VISTALL
 */
@ExtensionImpl
public class HeadlessSdkType extends SdkType {
    public static final String ID = "HEADLESS_SDK";

    public HeadlessSdkType() {
        super(ID, LocalizeValue.localizeTODO("Headless SDK"), Image.empty(16));
    }

    @Override
    public boolean isValidSdkHome(String path) {
        return true;
    }

    @Override
    public @Nullable String getVersionString(String sdkHome) {
        return "1.0";
    }
}
