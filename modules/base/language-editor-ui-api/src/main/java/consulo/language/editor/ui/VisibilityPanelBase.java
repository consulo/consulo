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
package consulo.language.editor.ui;

import consulo.proxy.EventDispatcher;
import consulo.ui.Component;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2024-09-10
 */
public abstract class VisibilityPanelBase<V> {
    public final EventDispatcher<VisibilityPanelListener> myEventDispatcher = EventDispatcher.create(VisibilityPanelListener.class);

    @Nullable
    public abstract V getVisibility();

    public abstract void setVisibility(V visibility);

    @Nonnull
    public abstract Component getComponent();

    public void addListener(VisibilityPanelListener listener) {
        myEventDispatcher.addListener(listener);
    }
}
