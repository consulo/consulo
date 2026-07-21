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
package consulo.it.internal.ui;

import consulo.ui.TextBoxWithExtensions;
import org.jspecify.annotations.Nullable;

/**
 * Dummy-but-creatable headless {@link TextBoxWithExtensions}.
 *
 * @author VISTALL
 */
public class HeadlessTextBoxWithExtensions extends HeadlessTextBox implements TextBoxWithExtensions {
    public HeadlessTextBoxWithExtensions(@Nullable String text) {
        super(text);
    }

    @Override
    public TextBoxWithExtensions setExtensions(Extension... extensions) {
        return this;
    }

    @Override
    public TextBoxWithExtensions addLastExtension(Extension extension) {
        return this;
    }

    @Override
    public TextBoxWithExtensions addFirstExtension(Extension extension) {
        return this;
    }
}
