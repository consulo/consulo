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

import consulo.ui.FontBox;

/**
 * Dummy-but-creatable headless {@link FontBox}.
 *
 * @author VISTALL
 */
public class HeadlessFontBox extends HeadlessValueComponentBase<String> implements FontBox {
    private boolean myMonospacedOnly;

    public HeadlessFontBox() {
        super(null);
    }

    @Override
    public void setMonospacedOnly(boolean monospacedOnly) {
        myMonospacedOnly = monospacedOnly;
    }

    @Override
    public boolean isMonospacedOnly() {
        return myMonospacedOnly;
    }

    @Override
    public boolean hasFocus() {
        return false;
    }

    @Override
    public void focus() {
    }

    @Override
    public void setFocusable(boolean focusable) {
    }

    @Override
    public boolean isFocusable() {
        return false;
    }
}
