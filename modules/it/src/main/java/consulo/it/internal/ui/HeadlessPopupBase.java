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
import consulo.disposer.Disposer;
import consulo.logging.Logger;
import consulo.ui.Component;
import consulo.ui.Popup;
import consulo.ui.PopupOptions;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.PopupCloseEvent;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 */
public abstract class HeadlessPopupBase extends HeadlessComponentBase implements Disposable {
    private static final Logger LOG = Logger.getInstance(HeadlessPopupBase.class);

    private final PopupOptions myOptions;

    private @Nullable String myTitle;
    private @Nullable Component myContent;
    private @Nullable Component myTarget;
    private int myMinimumWidth;
    private boolean myShown;
    private boolean myDisposed;

    protected HeadlessPopupBase(PopupOptions options) {
        myOptions = options;
    }

    public PopupOptions getOptions() {
        return myOptions;
    }

    @RequiredUIAccess
    public void setTitle(@Nullable String title) {
        myTitle = title;
    }

    public @Nullable String getTitle() {
        return myTitle;
    }

    @RequiredUIAccess
    public void setContent(Component content) {
        if (myContent instanceof HeadlessComponentBase previous && previous.getParent() == this) {
            previous.setParentComponent(null);
        }
        myContent = content;
        if (content instanceof HeadlessComponentBase child) {
            child.setParentComponent(this);
        }
    }

    public @Nullable Component getContent() {
        return myContent;
    }

    @RequiredUIAccess
    public void setMinimumWidth(int width) {
        myMinimumWidth = width;
    }

    public int getMinimumWidth() {
        return myMinimumWidth;
    }

    public @Nullable Component getTarget() {
        return myTarget;
    }

    public boolean isDisposed() {
        return myDisposed;
    }

    @RequiredUIAccess
    public void showAt(Component target, int x, int y, int anchorHeight) {
        show(target);
    }

    @RequiredUIAccess
    protected void show(@Nullable Component target) {
        if (myDisposed) {
            LOG.error("Popup already disposed");
            return;
        }

        myTarget = target;
        myShown = true;
    }

    @RequiredUIAccess
    public void close() {
        if (myDisposed) {
            return;
        }

        myDisposed = true;
        myShown = false;

        getListenerDispatcher(PopupCloseEvent.class).onEvent(new PopupCloseEvent((Popup) this));

        Disposer.dispose(this);
    }

    @Override
    public boolean isVisible() {
        return myShown && !myDisposed;
    }

    @Override
    public void dispose() {
        myDisposed = true;
        myShown = false;
    }
}
