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

import consulo.localize.LocalizeValue;
import consulo.ui.ex.action.AnAction;
import org.jspecify.annotations.Nullable;

import java.util.function.Function;

public sealed interface ErrorStatusPresenter<T> permits TextErrorStatusPresenter, HtmlErrorStatusPresenter {
    String ERROR_ACTION_HREF = "ERROR_ACTION";

    /**
     * @deprecated moved to {@link TextErrorStatusPresenter#getErrorTitle(Object)}
     */
    @Deprecated
    LocalizeValue getErrorTitle(T error);

    @Nullable
    AnAction getErrorAction(T error);

    static <T extends Throwable> TextErrorStatusPresenter<T> simple(LocalizeValue title) {
        return simple(title, null, error -> null);
    }

    static <T extends Throwable> TextErrorStatusPresenter<T> simple(LocalizeValue title,
                                                                    @Nullable Function<T, LocalizeValue> descriptionProvider,
                                                                    Function<T, @Nullable AnAction> actionProvider) {
        return new TextErrorStatusPresenter<>() {
            @Override
            public LocalizeValue getErrorTitle(T error) {
                return title;
            }

            @Override
            public @Nullable AnAction getErrorAction(T error) {
                return actionProvider.apply(error);
            }

            @Override
            public LocalizeValue getErrorDescription(T error) {
                if (descriptionProvider != null) {
                    return descriptionProvider.apply(error);
                }
                String message = error.getLocalizedMessage();
                return message == null ? LocalizeValue.empty() : LocalizeValue.of(message);
            }
        };
    }

    static <T extends Throwable> HtmlErrorStatusPresenter<T> simpleHTML(LocalizeValue title,
                                                                        @Nullable Function<T, String> descriptionProvider,
                                                                        Function<T, @Nullable AnAction> actionProvider) {
        return new HtmlErrorStatusPresenter<>() {
            @Override
            public LocalizeValue getErrorTitle(T error) {
                return LocalizeValue.empty();
            }

            @Override
            public @Nullable AnAction getErrorAction(T error) {
                return actionProvider.apply(error);
            }

            @Override
            public String getHTMLBody(T error) {
                String description = descriptionProvider != null ? descriptionProvider.apply(error) : error.getLocalizedMessage();
                return title.get() + "<br><br>" + (description == null ? "" : description);
            }
        };
    }
}
