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
package consulo.versionControlSystem.impl.internal.configurable;

import consulo.localize.LocalizeValue;
import consulo.ui.CheckBox;
import consulo.ui.Component;
import consulo.ui.IntBox;
import consulo.ui.Label;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;

/**
 * @author VISTALL
 * @since 2026-09-03
 */
public final class VcsSettingsRows {
    private VcsSettingsRows() {
    }

    @RequiredUIAccess
    public static Component gated(CheckBox gate, IntBox valueBox) {
        return gated(gate, valueBox, LocalizeValue.empty());
    }

    @RequiredUIAccess
    public static Component gated(CheckBox gate, IntBox valueBox, LocalizeValue suffix) {
        valueBox.setEnabled(gate.getValueOrError());
        gate.addValueListener(event -> valueBox.setEnabled(event.getValue()));

        HorizontalLayout right = HorizontalLayout.create(5).add(valueBox);
        if (suffix.isNotEmpty()) {
            right.add(Label.create(suffix));
        }
        return DockLayout.create().left(gate).right(right);
    }
}
