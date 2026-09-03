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

import consulo.disposer.Disposable;
import consulo.ui.Component;
import consulo.ui.HasSize;
import consulo.ui.Size2D;
import consulo.ui.BorderBuilder;
import consulo.ui.PaddingBuilder;
import consulo.ui.impl.BorderBuilderImpl;
import consulo.ui.impl.PaddingBuilderImpl;
import consulo.ui.color.ColorValue;
import consulo.ui.event.ComponentEvent;
import consulo.ui.event.ComponentEventListener;
import consulo.ui.font.Font;
import consulo.ui.font.FontManager;
import consulo.ui.impl.UIDataObject;
import consulo.util.dataholder.Key;
import org.jspecify.annotations.Nullable;

/**
 * Base for dummy-but-creatable headless {@code consulo.ui} components. Component-wide behaviour
 * (user data, listener dispatch, borders) is delegated to the shared {@link UIDataObject}, exactly
 * as the AWT/web backends do; visibility/enabled/size/font/parent are plain in-memory state.
 * <p>
 * These components never render; they only need to be constructable and hold trivial state so the
 * real {@code Unified*}/UI code can run headlessly.
 *
 * @author VISTALL
 */
public abstract class HeadlessComponentBase implements Component, HasSize {
    private final UIDataObject myDataObject = new UIDataObject();

    private boolean myVisible = true;
    private boolean myEnabled = true;
    private @Nullable Size2D mySize;
    private @Nullable Component myParent;

    protected void setParentComponent(@Nullable Component parent) {
        myParent = parent;
    }

    @Override
    public <T> void putUserData(Key<T> key, @Nullable T value) {
        myDataObject.putUserData(key, value);
    }

    @Override
    public <T> @Nullable T getUserData(Key<T> key) {
        return myDataObject.getUserData(key);
    }

    @Override
    public BorderBuilder borderBuilder() {
        return new BorderBuilderImpl(myDataObject, () -> {
        });
    }

    @Override
    public PaddingBuilder paddingBuilder() {
        return new PaddingBuilderImpl(myDataObject, () -> {
        });
    }

    @Override
    public boolean isVisible() {
        return myVisible;
    }

    @Override
    public void setVisible(boolean value) {
        myVisible = value;
    }

    @Override
    public boolean isEnabled() {
        return myEnabled;
    }

    @Override
    public void setEnabled(boolean value) {
        myEnabled = value;
    }

    @Override
    public @Nullable Component getParent() {
        return myParent;
    }

    @Override
    public void setSize(Size2D size) {
        mySize = size;
    }

    @Override
    public <C extends Component, E extends ComponentEvent<C>> Disposable addListener(
        Class<? extends E> eventClass,
        ComponentEventListener<C, E> listener
    ) {
        return myDataObject.addListener(eventClass, listener);
    }

    @Override
    public <C extends Component, E extends ComponentEvent<C>> ComponentEventListener<C, E> getListenerDispatcher(Class<E> eventClass) {
        return myDataObject.getDispatcher(eventClass);
    }

    public void setAccessibleName(consulo.localize.LocalizeValue name) {
    }

    public void setAccessibleDescription(consulo.localize.LocalizeValue description) {
    }
}
