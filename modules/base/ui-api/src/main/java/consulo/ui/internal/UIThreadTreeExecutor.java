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
package consulo.ui.internal;

import consulo.ui.Tree;
import consulo.ui.TreeExecutor;
import consulo.ui.UIAccess;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * What {@link TreeExecutor#uiThread()} answers. It holds nothing - the access is resolved from the tree given
 * at each call, since an access kept in a field would go stale and a frontend serves several UIs.
 *
 * @author VISTALL
 * @since 2026-08-29
 */
public final class UIThreadTreeExecutor implements TreeExecutor {
    public static final UIThreadTreeExecutor INSTANCE = new UIThreadTreeExecutor();

    private UIThreadTreeExecutor() {
    }

    @Override
    public <T> CompletableFuture<T> execute(Tree<?> tree, Supplier<T> task) {
        UIAccess access = tree.getUIAccess();
        if (access == null || !access.isValid()) {
            // a tree in no UI has nobody to show the result to
            CompletableFuture<T> rejected = new CompletableFuture<>();
            rejected.cancel(false);
            return rejected;
        }

        return access.giveAsync(task);
    }
}
