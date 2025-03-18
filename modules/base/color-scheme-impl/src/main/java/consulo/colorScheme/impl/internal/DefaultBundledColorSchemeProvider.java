/*
 * Copyright 2013-2022 consulo.io
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
package consulo.colorScheme.impl.internal;

import consulo.annotation.component.ExtensionImpl;
import consulo.colorScheme.BundledColorSchemeProvider;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 18-Jun-22
 */
@ExtensionImpl
public class DefaultBundledColorSchemeProvider implements BundledColorSchemeProvider {
    @Nonnull
    @Override
    public String[] getColorSchemeFiles() {
        return new String[]{
            "Default.xml",
            "IDEA.xml",
            "Consulo Light.xml",
            "Darcula.xml"
        };
    }
}
