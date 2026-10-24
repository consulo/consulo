// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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
package consulo.review.list.error;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.disposer.Disposable;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.concurrent.coroutine.ObservableValue;
import org.jspecify.annotations.Nullable;

/**
 * The status shown in place of content that failed to load: the error and the action the presenter offers about it.
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface ErrorStatusPanelFactory {
    @RequiredUIAccess
    <T> Component create(T error, ErrorStatusPresenter<T> errorPresenter, ErrorStatusAlignment alignment);

    @RequiredUIAccess
    default <T> Component create(T error, ErrorStatusPresenter<T> errorPresenter) {
        return create(error, errorPresenter, ErrorStatusAlignment.CENTER);
    }

    /**
     * Follows {@code errorState}, hidden while it holds no error.
     */
    @RequiredUIAccess
    <T> Component create(Disposable parentDisposable,
                         ObservableValue<@Nullable T> errorState,
                         ErrorStatusPresenter<T> errorPresenter,
                         ErrorStatusAlignment alignment);

    @RequiredUIAccess
    default <T> Component create(Disposable parentDisposable, ObservableValue<@Nullable T> errorState, ErrorStatusPresenter<T> errorPresenter) {
        return create(parentDisposable, errorState, errorPresenter, ErrorStatusAlignment.CENTER);
    }
}
