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
package consulo.localization.internal;

import consulo.localization.LocalizationManager;
import jakarta.annotation.Nonnull;

/**
 * @author UNV
 * @since 2025-11-18
 */
public abstract class CachingLocalizedValue extends AbstractLocalizedValue {
    @Nonnull
    protected final LocalizationManager myLocalizationManager;

    private String myText;
    private int myHashCode = -1;

    private byte myModificationCount = -1;

    protected CachingLocalizedValue(@Nonnull LocalizationManager manager) {
        myLocalizationManager = manager;
    }

    @Override
    public byte getModificationCount() {
        return myModificationCount;
    }

    @Nonnull
    @Override
    public final String getValue() {
        if (myModificationCount == myLocalizationManager.getModificationCount()) {
            return myText;
        }

        String newText = calcValue();

        myText = newText;
        myModificationCount = myLocalizationManager.getModificationCount();
        return newText;
    }

    @Nonnull
    protected abstract String calcValue();

    @Override
    public final int hashCode() {
        int hash = myHashCode;
        if (hash < 0) {
            hash = calcHashCode() & 0x7FFFFFFF;
            myHashCode = hash;
        }
        return hash;
    }

    protected abstract int calcHashCode();
}
