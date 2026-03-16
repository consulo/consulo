/*
 * Copyright 2013-2022 consulo.io
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
package consulo.desktop.awt.ui.impl.window;

import consulo.disposer.Disposable;
import consulo.ui.Component;
import consulo.ui.MenuBar;
import consulo.ui.Size2D;
import consulo.ui.Window;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.border.BorderPosition;
import consulo.ui.border.BorderStyle;
import consulo.ui.color.ColorValue;
import consulo.ui.event.ComponentEvent;
import consulo.ui.event.ComponentEventListener;
import consulo.ui.font.Font;
import consulo.util.dataholder.Key;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 06/01/2022
 */
public class DummyWindow implements Window {
    @RequiredUIAccess
    @Override
    public void setTitle(String title) {

    }

    @RequiredUIAccess
    @Override
    public void addBorder(BorderPosition borderPosition, BorderStyle borderStyle, @Nullable ColorValue colorValue, int width) {

    }

    @RequiredUIAccess
    @Override
    public void removeBorder(BorderPosition borderPosition) {

    }

    @Override
    public boolean isVisible() {
        return false;
    }

    @RequiredUIAccess
    @Override
    public void setVisible(boolean value) {

    }

    @Override
    public boolean isEnabled() {
        return false;
    }

    @RequiredUIAccess
    @Override
    public void setEnabled(boolean value) {

    }

    @Nullable
    @Override
    public Window getParent() {
        return null;
    }

    @RequiredUIAccess
    @Override
    public void setSize(Size2D size) {
    }

    @Override
    public Font getFont() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setFont(Font font) {

    }

    @Override
    public <C extends Component, E extends ComponentEvent<C>> ComponentEventListener<C, E> getListenerDispatcher(Class<E> eventClass) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <C extends Component, E extends ComponentEvent<C>> Disposable addListener(Class<? extends E> eventClass, ComponentEventListener<C, E> listener) {
        throw new UnsupportedOperationException();
    }

    @RequiredUIAccess
    @Override
    public void setContent(Component content) {

    }

    @RequiredUIAccess
    @Override
    public void setMenuBar(@Nullable MenuBar menuBar) {

    }

    @RequiredUIAccess
    @Override
    public void show() {

    }

    @RequiredUIAccess
    @Override
    public void close() {

    }

    @Override
    public boolean isActive() {
        return false;
    }

    @Nullable
    @Override
    public <T> T getUserData(Key<T> key) {
        return null;
    }

    @Override
    public <T> void putUserData(Key<T> key, @Nullable T value) {

    }

    @Override
    public void dispose() {

    }
}
