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
package consulo.ui.layout.event;

import consulo.ui.event.ComponentEvent;
import consulo.ui.event.details.InputDetails;
import consulo.ui.layout.FoldoutLayout;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2024-09-12
 */
public final class FoldoutLayoutOpenedEvent extends ComponentEvent<FoldoutLayout> {
    private final boolean myOpened;

    public FoldoutLayoutOpenedEvent(@Nonnull FoldoutLayout component, boolean opened) {
        this(component, null, opened);
    }

    public FoldoutLayoutOpenedEvent(@Nonnull FoldoutLayout component, @Nullable InputDetails inputDetails, boolean opened) {
        super(component, inputDetails);
        myOpened = opened;
    }

    public boolean isOpened() {
        return myOpened;
    }
}
