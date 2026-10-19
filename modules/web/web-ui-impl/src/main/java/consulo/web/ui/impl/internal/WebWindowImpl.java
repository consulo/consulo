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
package consulo.web.ui.impl.internal;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.dialog.DialogVariant;
import consulo.disposer.Disposer;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.Component;
import consulo.ui.MenuBar;
import consulo.ui.Toast;
import consulo.ui.UIAccess;
import consulo.ui.Window;
import consulo.ui.WindowOptions;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.WindowCloseEvent;
import consulo.ui.internal.ToastOverlay;
import consulo.util.lang.StringUtil;
import consulo.web.ui.impl.internal.base.ComponentHolder;
import consulo.web.ui.impl.internal.base.FromVaadinComponentWrapper;
import consulo.web.ui.impl.internal.base.TargetVaadin;
import consulo.web.ui.impl.internal.base.VaadinComponentDelegate;
import consulo.web.ui.impl.internal.base.WebFocusTracker;
import consulo.web.ui.impl.internal.image.WebImageConverter;
import consulo.web.ui.impl.internal.vaadin.VaadinSizeUtil;
import consulo.ui.Length;
import consulo.web.ui.impl.internal.vaadin.WebLength;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2017-09-15
 */
public class WebWindowImpl extends VaadinComponentDelegate<WebWindowImpl.Vaadin> implements Window, ToastOverlay {
    private static final String SIZE_STORAGE_PREFIX = "consulo.window.size.";

    private static final String RESTORE_SIZE_SCRIPT = "try { const size = JSON.parse(localStorage.getItem($0) || 'null');"
        + " if (size && size.width > 0 && size.height > 0) { this.width = size.width + 'px'; this.height = size.height + 'px'; } }"
        + " catch (e) { }";

    private static final String STORE_SIZE_SCRIPT =
        "try { localStorage.setItem($0, JSON.stringify({width: $1, height: $2})); } catch (e) { }";

    @StyleSheet("/button/webButton.css")
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
    private final boolean myUndecorated;
    private final boolean myReattachable;
    private final @Nullable WebWindowImpl myOwner;
    private final List<WebWindowImpl> myOwnedWindows = new CopyOnWriteArrayList<>();
    private final @Nullable String myDimensionKey;
    private final WebRootPaneImpl myRootPanel = new WebRootPaneImpl();
    private @Nullable WebToastStack myToastStack;
    private @Nullable Component myFocusedBeforeShow;
    private @Nullable Consumer<Window> myCloseHandler;

    public WebWindowImpl(boolean modal, WindowOptions options) {
        Vaadin vaadinComponent = getVaadinComponent();

        vaadinComponent.setModal(modal);
        vaadinComponent.setResizable(options.isResizable());
        vaadinComponent.setCloseOnEsc(false);
        vaadinComponent.setCloseOnOutsideClick(false);
        vaadinComponent.setDraggable(true);

        myUndecorated = options.isUndecorated();
        myDimensionKey = options.getDimensionKey();
        myOwner = options.getOwner() instanceof WebWindowImpl owner ? owner : null;
        myReattachable = options.isReattachable() || myOwner != null && myOwner.myReattachable;
        if (myOwner != null) {
            myOwner.myOwnedWindows.add(this);
        }
        if (myUndecorated) {
            vaadinComponent.addThemeVariants(DialogVariant.NO_PADDING);
        }
        else if (options.isClosable()) {
            addCloseDialogButton();
        }

        VaadinSizeUtil.setSizeFull(myRootPanel.getComponent());
        vaadinComponent.add(TargetVaadin.to(myRootPanel.getComponent()));

        if (myDimensionKey != null) {
            vaadinComponent.addResizeListener(event -> rememberSize(event.getWidth(), event.getHeight()));
        }

        vaadinComponent.addOpenedChangeListener(event -> {
            if (event.isFromClient() && !event.isOpened()) {
                closed();
            }
        });

        vaadinComponent.addDetachListener(event -> {
            if (!myReattachable && event.getUI().isClosing()) {
                fireClosed();
            }
        });

        WebFocusManagerImpl.register(toVaadinComponent());
        WebFocusTracker.installRoot(vaadinComponent.getElement());
    }

    private void addCloseDialogButton() {
        Button closeButton = new Button(WebImageConverter.getImage(PlatformIconGroup.actionsClose()), e -> requestClose());
        // created outside a VaadinComponentDelegate, so the small variant has to be added by hand here
        closeButton.addThemeVariants(ButtonVariant.TERTIARY, ButtonVariant.SMALL);
        closeButton.addClassName("consulo-toolbar-button");
        closeButton.addClassName("consulo-window-close");
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
            applyRememberedSize();

            UI.getCurrent().add(vaadin);
        }

        if (!vaadin.isOpened()) {
            myFocusedBeforeShow = WebFocusTracker.getFocusedComponent();
        }

        vaadin.open();
    }

    @RequiredUIAccess
    private void applyRememberedSize() {
        String dimensionKey = myDimensionKey;
        if (dimensionKey == null) {
            return;
        }

        getVaadinComponent().getElement().executeJs(RESTORE_SIZE_SCRIPT, SIZE_STORAGE_PREFIX + dimensionKey);
    }

    private void rememberSize(@Nullable String width, @Nullable String height) {
        String dimensionKey = myDimensionKey;
        int widthInPixels = toPixels(width);
        int heightInPixels = toPixels(height);
        if (dimensionKey == null || widthInPixels <= 0 || heightInPixels <= 0) {
            return;
        }

        getVaadinComponent().getElement().executeJs(STORE_SIZE_SCRIPT, SIZE_STORAGE_PREFIX + dimensionKey, widthInPixels, heightInPixels);
    }

    private static int toPixels(@Nullable String cssSize) {
        if (cssSize == null || !cssSize.endsWith("px")) {
            return -1;
        }

        try {
            return Math.round(Float.parseFloat(cssSize.substring(0, cssSize.length() - 2)));
        }
        catch (NumberFormatException e) {
            return -1;
        }
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

    @RequiredUIAccess
    @Override
    public void setCloseHandler(@Nullable Consumer<Window> closeHandler) {
        myCloseHandler = closeHandler;
    }

    @RequiredUIAccess
    private void requestClose() {
        Consumer<Window> closeHandler = myCloseHandler;
        if (closeHandler != null && !myDisposed) {
            closeHandler.accept(this);
            return;
        }

        close();
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

        // vaadin detaches an auto added overlay only when the browser reports back the 'closed' dom event, and it
        // nests an overlay opened on top of another modal inside that modal - so a window closed out of order stays
        // attached and keeps the ui element inert. detaching here runs the modal stack cleanup right away
        getVaadinComponent().getElement().removeFromParent();

        if (WebFocusTracker.getFocusedComponent() == null) {
            WebFocusTracker.setFocusedComponent(myFocusedBeforeShow);
        }
        myFocusedBeforeShow = null;

        fireClosed();
    }

    @RequiredUIAccess
    private void fireClosed() {
        if (myDisposed) {
            return;
        }

        myDisposed = true;

        if (myOwner != null) {
            myOwner.myOwnedWindows.remove(this);
        }

        getListenerDispatcher(WindowCloseEvent.class).onEvent(new WindowCloseEvent(this));

        Disposer.dispose(this);
    }

    @RequiredUIAccess
    public CompletableFuture<?> moveTo(WebUIAccessImpl target) {
        UI targetUI = target.getUI();
        UI previous = getVaadinComponent().getUI().orElse(null);
        if (myDisposed || previous == targetUI) {
            return CompletableFuture.completedFuture(null);
        }

        if (previous != null && !previous.isClosing() && previous.getSession() != null) {
            UIAccess previousAccess = VaadinComponentDelegate.getUIAccess(previous);
            return previousAccess.giveAsync(() -> {
                previous.push();

                releaseFromTree();
            }).thenCompose(ignored -> target.giveAsync(() -> attachTo(targetUI)));
        }

        releaseFromTree();
        attachTo(targetUI);
        return CompletableFuture.completedFuture(null);
    }

    private void releaseFromTree() {
        for (WebWindowImpl owned : myOwnedWindows) {
            owned.releaseFromTree();
        }

        getVaadinComponent().getElement().removeFromTree(false);
    }

    @RequiredUIAccess
    private void attachTo(UI ui) {
        if (myDisposed) {
            return;
        }

        Vaadin vaadin = getVaadinComponent();
        ui.add(vaadin);
        vaadin.open();

        for (WebWindowImpl owned : myOwnedWindows) {
            owned.attachTo(ui);
        }
    }

    @Override
    @RequiredUIAccess
    public void setTitle(String title) {
        if (myUndecorated) {
            return;
        }

        getVaadinComponent().setHeaderTitle(StringUtil.notNullize(title));
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
            stack = myToastStack = new WebToastStack(getVaadinComponent());
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

    @RequiredUIAccess
    @Override
    public void setWidth(Length width) {
        getVaadinComponent().setWidth(WebLength.toWindowCss(width));
    }

    @RequiredUIAccess
    @Override
    public void setHeight(Length height) {
        getVaadinComponent().setHeight(WebLength.toWindowCss(height));
    }
}
