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
package consulo.web.internal.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.icon.VaadinIcon;
import consulo.disposer.Disposer;
import consulo.ui.Component;
import consulo.ui.MenuBar;
import consulo.ui.Window;
import consulo.ui.WindowOptions;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.WindowCloseEvent;
import consulo.web.internal.ui.base.ComponentHolder;
import consulo.web.internal.ui.base.FromVaadinComponentWrapper;
import consulo.web.internal.ui.base.TargetVaadin;
import consulo.web.internal.ui.base.VaadinComponentDelegate;
import consulo.web.internal.ui.vaadin.VaadinSizeUtil;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2017-09-15
 */
public class WebWindowImpl extends VaadinComponentDelegate<WebWindowImpl.Vaadin> implements Window {
    public static class Vaadin extends Dialog implements ComponentHolder, FromVaadinComponentWrapper {
        private Component myComponent;

        @Override
        public void setComponent(Component component) {
            myComponent = component;
        }

        @Override
        public Component toUIComponent() {
            return myComponent;
        }
    }

    private boolean myDisposed;
    private final WebRootPaneImpl myRootPanel = new WebRootPaneImpl();

    public WebWindowImpl(boolean modal, WindowOptions options) {
        Vaadin vaadinComponent = getVaadinComponent();

        vaadinComponent.setModal(modal);
        vaadinComponent.setResizable(options.isResizable());
        vaadinComponent.setCloseOnEsc(false);
        vaadinComponent.setCloseOnOutsideClick(false);
        vaadinComponent.setDraggable(true);
        if (options.isClosable()) {
            addCloseDialogButton();
        }

        VaadinSizeUtil.setSizeFull(myRootPanel.getComponent());
        vaadinComponent.add(TargetVaadin.to(myRootPanel.getComponent()));

        vaadinComponent.addOpenedChangeListener(event -> {
            if (!event.isOpened()) {
                closed();
            }
        });

        WebFocusManagerImpl.register(toVaadinComponent());
    }

    private void addCloseDialogButton() {
        // the lumo iconset is not part of the aura theme used by the app shell
        Button closeButton = new Button(VaadinIcon.CLOSE_SMALL.create(), e -> close());
        // created outside a VaadinComponentDelegate, so the small variant has to be added by hand here
        closeButton.addThemeVariants(ButtonVariant.TERTIARY, ButtonVariant.SMALL);
        getVaadinComponent().getHeader().add(closeButton);
    }

    @Override
    public Vaadin createVaadinComponent() {
        return new Vaadin();
    }

    @Override
    @RequiredUIAccess
    public void show() {
        if (myDisposed) {
            throw new IllegalArgumentException("Window already disposed");
        }

        Vaadin vaadin = toVaadinComponent();
        if (!vaadin.isAttached()) {
            UI.getCurrent().add(vaadin);
        }

        vaadin.open();
    }

    @Override
    @RequiredUIAccess
    public void close() {
        if (myDisposed) {
            return;
        }

        getVaadinComponent().close();

        closed();
    }

    /**
     * Runs for both an api close and a close initiated from the browser, so anything registered through
     * {@link #addCloseListener} sees every close.
     */
    @RequiredUIAccess
    private void closed() {
        if (myDisposed) {
            return;
        }

        myDisposed = true;

        // vaadin detaches an auto added overlay only when the browser reports back the 'closed' dom event, and it
        // nests an overlay opened on top of another modal inside that modal - so a window closed out of order stays
        // attached and keeps the ui element inert. detaching here runs the modal stack cleanup right away
        getVaadinComponent().getElement().removeFromParent();

        getListenerDispatcher(WindowCloseEvent.class).onEvent(new WindowCloseEvent(this));

        Disposer.dispose(this);
    }

    @Override
    @RequiredUIAccess
    public void setTitle(String title) {
        getVaadinComponent().setHeaderTitle(title);
    }

    @Override
    @RequiredUIAccess
    public void setContent(Component content) {
        myRootPanel.setCenterComponent(content);
    }

    @Override
    @RequiredUIAccess
    public void setMenuBar(@Nullable MenuBar menuBar) {
        myRootPanel.setMenuBar(menuBar);
    }

    @Override
    public boolean isActive() {
        return true;
    }

    @Override
    public void dispose() {
    }

    @Override
    public @Nullable Window getParent() {
        return (Window) super.getParent();
    }
}