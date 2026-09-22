// File: ImmediateConfigurable.java
// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor.impl.internal.inlay.setting;

import consulo.localize.LocalizeValue;

import javax.swing.*;
import java.util.Collections;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public interface ImmediateConfigurable {
    JComponent createComponent(ChangeListener listener);

    default void reset() {
    }

    default List<Case> getCases() {
        return Collections.emptyList();
    }

    record Case(
        LocalizeValue name,
        String id,
        BooleanSupplier loadFromSettings,
        Consumer<Boolean> onUserChanged,
        LocalizeValue extendedDescription
    ) {
        public boolean getValue() {
            return loadFromSettings.getAsBoolean();
        }

        public void setValue(boolean value) {
            onUserChanged.accept(value);
        }
    }
}