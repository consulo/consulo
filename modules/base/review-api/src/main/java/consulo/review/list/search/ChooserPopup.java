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
package consulo.review.list.search;

import consulo.ui.ex.popup.JBPopup;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * A chooser popup which is not shown yet, and the choice it completes with once it is closed.
 * Cancelling the result closes the popup.
 *
 * @author VISTALL
 * @since 2026-10-24
 */
public record ChooserPopup<R extends @Nullable Object>(JBPopup popup, CompletableFuture<R> result) {
}
