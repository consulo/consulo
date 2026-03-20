/*
 * Copyright 2013-2021 consulo.io
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
package consulo.desktop.swt.ui.impl;

import consulo.desktop.swt.ui.impl.layout.DesktopSwtScrollableLayoutImpl;
import consulo.desktop.swt.ui.impl.layout.data.LayoutDataWithSize;
import consulo.disposer.Disposable;
import consulo.ui.Component;
import consulo.ui.Size2D;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.border.BorderPosition;
import consulo.ui.border.BorderStyle;
import consulo.ui.color.ColorValue;
import consulo.ui.event.ComponentEvent;
import consulo.ui.event.ComponentEventListener;
import consulo.ui.font.Font;
import consulo.ui.impl.UIDataObject;
import consulo.util.dataholder.Key;
import org.jspecify.annotations.Nullable;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Widget;

import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2021-04-29
 */
public abstract class SWTComponentDelegate<SWT extends Widget> implements Component {
    public static final String UI_COMPONENT_KEY = "UI_COMPONENT_KEY";

    protected Size2D mySize;

    protected UIDataObject myDataObject = new UIDataObject();

    protected SWT myComponent;

    private boolean myEnabled = true;
    private boolean myVisible = true;

    public final void bind(Composite parent, Object layoutData) {
        myComponent = createSWT(parent);

        if (myComponent instanceof Control control) {
            control.setLayoutData(layoutData);
            control.setEnabled(myEnabled);
            control.setVisible(myVisible);
            control.setData(UI_COMPONENT_KEY, this);
            if (mySize != null) {
                if (layoutData instanceof LayoutDataWithSize) {
                    ((LayoutDataWithSize) layoutData).setSize(mySize);
                }
            }
        }

        initialize(myComponent);
    }

    protected static int packScrollFlags(Composite parent, int flags) {
        Object data = parent.getData(UI_COMPONENT_KEY);
        if (data instanceof DesktopSwtScrollableLayoutImpl) {
            flags |= org.eclipse.swt.SWT.V_SCROLL;
            flags |= org.eclipse.swt.SWT.H_SCROLL;
            return flags;
        }

        return flags;
    }

    public SWT toSWTComponent() {
        return myComponent;
    }

    protected abstract SWT createSWT(Composite parent);

    protected void initialize(SWT component) {
    }

    public final Composite getComposite() {
        if (myComponent instanceof Composite) {
            return (Composite) myComponent;
        }

        throw new UnsupportedOperationException(getClass().getName());
    }

    public void setParent(@Nullable Component component) {
        disposeSWT();
    }

    public void disposeSWT() {
        if (myComponent != null) {
            myComponent.dispose();
            myComponent = null;
        }
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
        return myVisible;
    }

    @RequiredUIAccess
    @Override
    public void setVisible(boolean value) {
        myVisible = value;

        if (myComponent instanceof Control control) {
            control.setVisible(myVisible);
        }
    }

    @Override
    public boolean isEnabled() {
        return myEnabled;
    }

    @RequiredUIAccess
    @Override
    public void setEnabled(boolean value) {
        myEnabled = value;

        if (myComponent instanceof Control control) {
            control.setEnabled(value);
        }
    }

    @Override
    public @Nullable Component getParent() {
        return null;
    }

    @RequiredUIAccess
    @Override
    public void setSize(Size2D size) {
        mySize = size;

        SWT swt = toSWTComponent();
        if (swt instanceof Control control) {
            Object layoutData = control.getLayoutData();
            if (layoutData instanceof LayoutDataWithSize layoutDataWithSize) {
                layoutDataWithSize.setSize(size);

                control.getParent().requestLayout();
            }
        }
    }

    
    @Override
    public Disposable addUserDataProvider(Function<Key<?>, Object> function) {
        return myDataObject.addUserDataProvider(function);
    }

    
    @Override
    public Font getFont() {
        return null;
    }

    @Override
    public void setFont(Font font) {

    }

    
    @Override
    public <C extends Component, E extends ComponentEvent<C>> ComponentEventListener<C, E> getListenerDispatcher(Class<E> eventClass) {
        return myDataObject.getDispatcher(eventClass);
    }

    
    @Override
    public <C extends Component, E extends ComponentEvent<C>> Disposable addListener(Class<? extends E> eventClass,
                                                                                     ComponentEventListener<C, E> listener) {
        return myDataObject.addListener(eventClass, listener);
    }

    @Override
    public <T> @Nullable T getUserData(Key<T> key) {
        return myDataObject.getUserData(key);
    }

    @Override
    public <T> void putUserData(Key<T> key, @Nullable T value) {
        myDataObject.putUserData(key, value);
    }

    public void focus() {
    }

    public boolean hasFocus() {
        return false;
    }

    public void setFocusable(boolean focusable) {

    }

    public boolean isFocusable() {
        return true;
    }
}
