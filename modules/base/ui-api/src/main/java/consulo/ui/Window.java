/*
 * Copyright 2013-2016 consulo.io
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
package consulo.ui;

import consulo.disposer.Disposable;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ComponentEventListener;
import consulo.ui.event.WindowCloseEvent;
import consulo.ui.internal.UIInternal;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2016-06-14
 */
public interface Window extends Component, Disposable, HasSize {
    static Window create(String title, WindowOptions options) {
        return UIInternal.get()._Window_create(title, options);
    }

    static @Nullable Window getActiveWindow() {
        return UIInternal.get()._Window_getActiveWindow();
    }

    static @Nullable Window getFocusedWindow() {
        return UIInternal.get()._Window_getFocusedWindow();
    }

    @RequiredUIAccess
    void setTitle(String title);

    @Override
    @Nullable Window getParent();

    @RequiredUIAccess
    void setContent(Component content);

    @RequiredUIAccess
    void setMenuBar(@Nullable MenuBar menuBar);

    /**
     * Doesn't block current thread.
     */
    @RequiredUIAccess
    void show();

    /**
     * Close and dispose resources. Window can't be opened second time.
     */
    @RequiredUIAccess
    void close();

    boolean isActive();

    /**
     * Takes over the close the user asks for, through the close button of the window: the handler runs instead and
     * decides whether to call {@link #close()}. Without a handler such a request closes the window.
     */
    @RequiredUIAccess
    void setCloseHandler(@Nullable Consumer<Window> closeHandler);

    default Disposable addCloseListener(ComponentEventListener<Window, WindowCloseEvent> listener) {
        return addListener(WindowCloseEvent.class, listener);
    }
}
