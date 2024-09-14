/*
 * Copyright 2013-2019 consulo.io
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
import consulo.ui.event.BlurEvent;
import consulo.ui.event.ComponentEventListener;
import consulo.ui.event.FocusEvent;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2019-11-09
 */
public interface FocusableComponent extends Component {
    static boolean hasFocus(@Nonnull Component component) {
        return component instanceof FocusableComponent f && f.hasFocus();
    }

    boolean hasFocus();

    void focus();

    void setFocusable(boolean focusable);

    boolean isFocusable();

    @Nonnull
    default Disposable addFocusListener(@Nonnull ComponentEventListener<FocusableComponent, FocusEvent> listener) {
        return addListener(FocusEvent.class, listener);
    }

    @Nonnull
    default Disposable addBlurListener(@Nonnull ComponentEventListener<FocusableComponent, BlurEvent> listener) {
        return addListener(BlurEvent.class, listener);
    }
}