/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.colorScheme.setting;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;

/**
 * Defines interface for extending set of text/color descriptors operated by color schemes.
 *
 * @author Denis Zhdanov
 * @since 2012-01-19
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface ColorAndFontDescriptorsProvider extends ColorAndFontDescriptors {
    ExtensionPointName<ColorAndFontDescriptorsProvider> EP_NAME = ExtensionPointName.create(ColorAndFontDescriptorsProvider.class);
}
