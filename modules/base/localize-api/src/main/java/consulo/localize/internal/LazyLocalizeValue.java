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
package consulo.localize.internal;

import consulo.localize.LocalizeKey;
import consulo.localize.LocalizeValue;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2026-05-03
 */
public class LazyLocalizeValue implements LocalizeValue {
    private static final ConstantLocalizeValue STUB = new ConstantLocalizeValue("__STUB__");

    private final Supplier<LocalizeValue> myBuilder;

    private volatile LocalizeValue myValue = STUB;

    public LazyLocalizeValue(Supplier<LocalizeValue> builder) {
        myBuilder = builder;
    }

    private LocalizeValue resolve() {
        if (myValue == STUB) {
            return myValue = myBuilder.get();
        }
        else {
            return myValue;
        }
    }

    @Override
    public Optional<LocalizeKey> getKey() {
        return resolve().getKey();
    }

    @Override
    public String getId() {
        return resolve().getId();
    }

    @Override
    public String getValue() {
        return resolve().getValue();
    }

    @Override
    public byte getModificationCount() {
        return resolve().getModificationCount();
    }
}
