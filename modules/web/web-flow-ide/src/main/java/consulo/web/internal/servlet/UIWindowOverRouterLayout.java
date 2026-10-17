/*
 * Copyright 2013-2017 consulo.io
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
package consulo.web.internal.servlet;

import com.vaadin.flow.component.UI;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ui.Component;
import consulo.ui.MenuBar;
import consulo.ui.Size2D;
import consulo.ui.Toast;
import consulo.ui.UIAccess;
import consulo.ui.Window;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.BorderBuilder;
import consulo.ui.PaddingBuilder;
import consulo.ui.impl.BorderBuilderImpl;
import consulo.ui.impl.PaddingBuilderImpl;
import consulo.ui.color.ColorValue;
import consulo.ui.event.ComponentEvent;
import consulo.ui.event.ComponentEventListener;
import consulo.ui.font.Font;
import consulo.ui.font.FontManager;
import consulo.ui.internal.ToastOverlay;
import consulo.util.dataholder.Key;
import consulo.util.dataholder.UserDataHolderBase;
import consulo.web.ui.impl.internal.WebRootPaneImpl;
import consulo.web.ui.impl.internal.WebToastStack;
import consulo.web.ui.impl.internal.base.TargetVaadin;
import consulo.web.ui.impl.internal.base.VaadinComponentDelegate;
import org.jspecify.annotations.Nullable;


/**
 * @author VISTALL
 * @since 2017-09-11
 */
class UIWindowOverRouterLayout extends UserDataHolderBase implements Window, ToastOverlay {
    private final VaadinRootLayout myRouterLayout;

    private final WebRootPaneImpl myRootPanel = new WebRootPaneImpl();

    private boolean myDisposed;

    private @Nullable WebToastStack myToastStack;

    @RequiredUIAccess
    public UIWindowOverRouterLayout(VaadinRootLayout routerLayout) {
        myRouterLayout = routerLayout;
        routerLayout.add(TargetVaadin.to(myRootPanel.getComponent()));
    }

    @RequiredUIAccess
    @Override
    public void showToast(Toast toast) {
        toastStack().show(toast);
    }

    @RequiredUIAccess
    @Override
    public void setToastArea(@Nullable Component area) {
        toastStack().setArea(area == null ? null : TargetVaadin.to(area));
    }

    private WebToastStack toastStack() {
        WebToastStack stack = myToastStack;
        if (stack == null) {
            stack = myToastStack = new WebToastStack(myRouterLayout);
        }
        return stack;
    }

    @RequiredUIAccess
    @Override
    public void hideToast(Toast toast) {
        WebToastStack stack = myToastStack;
        if (stack != null) {
            stack.hide(toast);
        }
    }

    @Override
    public @Nullable UIAccess getUIAccess() {
        return myRouterLayout.getUI().map(VaadinComponentDelegate::getUIAccess).orElse(null);
    }

    @Override
    public void dispose() {
        myDisposed = true;
    }

    @RequiredUIAccess
    @Override
    public BorderBuilder borderBuilder() {
        return BorderBuilderImpl.NOOP;
    }

    @Override
    public PaddingBuilder paddingBuilder() {
        return PaddingBuilderImpl.NOOP;
    }


    @Override
    public boolean isVisible() {
        return true;
    }

    @RequiredUIAccess
    @Override
    public void setVisible(boolean value) {
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @RequiredUIAccess
    @Override
    public void setEnabled(boolean value) {
    }

    @Override
    public @Nullable Window getParent() {
        return null;
    }

    @RequiredUIAccess
    @Override
    public void setSize(Size2D size) {

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
    public void setTitle(String title) {
        UI.getCurrent().getPage().setTitle(title);
    }

    @RequiredUIAccess
    @Override
    public void setContent(Component content) {
        if (myDisposed) {
            throw new IllegalArgumentException("Already disposed");
        }

        myRootPanel.setCenterComponent(content);
    }

    @RequiredUIAccess
    @Override
    public void setMenuBar(@Nullable MenuBar menuBar) {
        myRootPanel.setMenuBar(menuBar);
    }

    @RequiredUIAccess
    @Override
    public void show() {
        throw new UnsupportedOperationException();
    }

    @RequiredUIAccess
    @Override
    public void close() {
        Disposer.dispose(this);
    }

    @Override
    public boolean isActive() {
        return true;
    }
}
