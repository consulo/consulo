// File: ImmediateConfigurable.java
// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor.impl.internal.inlay.setting;

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

    class Case {
        private final String name;
        private final String id;
        private final BooleanSupplier loadFromSettings;
        private final Consumer<Boolean> onUserChanged;
        private final String extendedDescription;

        public Case(String name,
                    String id,
                    BooleanSupplier loadFromSettings,
                    Consumer<Boolean> onUserChanged,
                    String extendedDescription) {
            this.name = name;
            this.id = id;
            this.loadFromSettings = loadFromSettings;
            this.onUserChanged = onUserChanged;
            this.extendedDescription = extendedDescription;
        }

        public String getName() {
            return name;
        }

        public String getId() {
            return id;
        }

        public boolean getValue() {
            return loadFromSettings.getAsBoolean();
        }

        public void setValue(boolean value) {
            onUserChanged.accept(value);
        }

        public String getExtendedDescription() {
            return extendedDescription;
        }
    }
}