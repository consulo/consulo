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
package consulo.application.impl.internal;

import consulo.application.Application;
import consulo.ui.ModalityState;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.internal.UIAccessInternal;

/**
 * @author VISTALL
 * @since 2026-10-12
 */
public interface ModalityUIAccess extends UIAccessInternal {
    @Override
    default void giveLater(@RequiredUIAccess Runnable runnable, ModalityState modalityState) {
        LaterInvocator.of(this).invokeLaterWithCallback(runnable, modalityState, Application.get().getDisposed(), null);
    }

    @RequiredUIAccess
    @Override
    default boolean isInModalContext() {
        return LaterInvocator.of(this).isInModalContext();
    }

    @RequiredUIAccess
    @Override
    default ModalityState getCurrentModalityState() {
        return LaterInvocator.of(this).getCurrentModalityState();
    }

    @RequiredUIAccess
    @Override
    default void enterModal(Object modalEntity) {
        LaterInvocator.of(this).enterModal(modalEntity);
    }

    @RequiredUIAccess
    @Override
    default void leaveModal(Object modalEntity) {
        LaterInvocator.of(this).leaveModal(modalEntity);
    }
}
