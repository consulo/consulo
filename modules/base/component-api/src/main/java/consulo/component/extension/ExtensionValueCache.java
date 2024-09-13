/*
 * Copyright 2013-2024 consulo.io
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
package consulo.component.extension;

import consulo.util.lang.ref.SimpleReference;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2024-09-13
 */
public class ExtensionValueCache<E> implements Supplier<E> {
    private final ExtensionPoint<E> myExtensionPoint;
    private final SimpleReference<E> myValueRef;

    private final long myCachedModificationCount;

    public ExtensionValueCache(@Nonnull ExtensionPoint<E> extensionPoint, @Nullable E definition) {
        myExtensionPoint = extensionPoint;
        myValueRef = new SimpleReference<>(definition);

        myCachedModificationCount = extensionPoint.getModificationCount();
    }

    @Override
    public E get() {
        if (myExtensionPoint.getModificationCount() != myCachedModificationCount) {
            return null;
        }

        return myValueRef.get();
    }
}
