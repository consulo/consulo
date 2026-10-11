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
package consulo.ui.ex.dialog;

import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import org.jspecify.annotations.Nullable;

/**
 * A dialog with a component along its left edge across the whole height, beside the center component and the
 * buttons.
 *
 * @author VISTALL
 * @since 2026-10-11
 */
public abstract class WholeLeftDialogDescriptor extends DialogDescriptor {
    public WholeLeftDialogDescriptor(LocalizeValue title) {
        super(title);
    }

    @RequiredUIAccess
    public abstract Component createLeftComponent(Disposable uiDisposable);

    /**
     * The key the share of the left component is remembered under, or {@code null} if it is not remembered.
     */
    public @Nullable String getLeftSplitterKey() {
        return getClass().getName();
    }
}
