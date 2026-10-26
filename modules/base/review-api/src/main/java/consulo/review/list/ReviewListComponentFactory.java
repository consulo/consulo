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
package consulo.review.list;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.dataContext.DataSink;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionGroup;
import org.jspecify.annotations.Nullable;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * The review list of a review tool window: a row per review, and the loading, empty and error states in its place.
 *
 * @author VISTALL
 * @since 2026-10-18
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface ReviewListComponentFactory {
    /**
     * @param projectName shown by the empty state
     * @param opener      called on a double click or Enter
     */
    @RequiredUIAccess
    default <T> Component create(Disposable parentDisposable,
                                 LocalizeValue projectName,
                                 CodeReviewListItemsViewModel<T> listVm,
                                 @RequiredUIAccess Consumer<T> opener) {
        return create(parentDisposable, projectName, listVm, opener, null, (sink, item) -> {
        });
    }

    /**
     * @param popupGroup           the context menu of a row, or {@code null} for none
     * @param selectedItemProvider fills the data context of the list with the selected review, {@code null} while none is selected
     */
    @RequiredUIAccess
    <T> Component create(Disposable parentDisposable,
                         LocalizeValue projectName,
                         CodeReviewListItemsViewModel<T> listVm,
                         @RequiredUIAccess Consumer<T> opener,
                         @Nullable ActionGroup popupGroup,
                         BiConsumer<DataSink, @Nullable T> selectedItemProvider);
}
