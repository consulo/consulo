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
package consulo.language.editor.impl.internal.inspection.setting;

import consulo.colorScheme.EditorColorsScheme;
import consulo.colorScheme.TextAttributes;
import consulo.colorScheme.TextAttributesKey;
import consulo.colorScheme.ui.TextAttributesDescription;
import consulo.localize.LocalizeValue;

/**
 * @author VISTALL
 * @since 2026-10-11
 */
public class SeverityTextAttributesDescription extends TextAttributesDescription {
    public SeverityTextAttributesDescription(LocalizeValue name, TextAttributes attributes, TextAttributesKey type) {
        super(name, LocalizeValue.empty(), attributes, type, null, null, null);
    }

    @Override
    public void apply(EditorColorsScheme scheme) {
    }

    @Override
    public boolean isErrorStripeEnabled() {
        return true;
    }

    @Override
    public TextAttributes getTextAttributes() {
        return super.getTextAttributes();
    }
}
