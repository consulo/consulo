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

import consulo.ui.ValueComponent;
import org.jspecify.annotations.Nullable;

/**
 * Base for dummy-but-creatable headless {@link ValueComponent}s: holds a single value field.
 *
 * @author VISTALL
 */
public abstract class HeadlessValueComponentBase<V> extends HeadlessComponentBase implements ValueComponent<V> {
    private @Nullable V myValue;

    public HeadlessValueComponentBase() {
    }

    public HeadlessValueComponentBase(@Nullable V value) {
        myValue = value;
    }

    @Override
    public @Nullable V getValue() {
        return myValue;
    }

    @Override
    public void setValue(@Nullable V value, boolean fireListeners) {
        myValue = value;
    }
}
