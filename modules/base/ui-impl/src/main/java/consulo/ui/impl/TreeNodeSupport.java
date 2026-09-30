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
package consulo.ui.impl;

import consulo.component.ProcessCanceledException;
import consulo.logging.Logger;
import consulo.ui.TreeNode;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Predicate;

/**
 * @author VISTALL
 * @since 2026-09-30
 */
public final class TreeNodeSupport {
    private TreeNodeSupport() {
    }

    public static <E> @Nullable TreeNode<E> findFirst(List<? extends TreeNode<E>> children, Predicate<E> predicate) {
        for (TreeNode<E> child : children) {
            if (predicate.test(child.getValue())) {
                return child;
            }
        }
        return null;
    }

    public static <E> CompletableFuture<TreeNode<E>> findDeep(List<? extends TreeNode<E>> children, Predicate<E> predicate) {
        return findDeep(children, predicate, 0);
    }

    private static <E> CompletableFuture<TreeNode<E>> findDeep(List<? extends TreeNode<E>> children, Predicate<E> predicate, int from) {
        for (int index = from; index < children.size(); index++) {
            TreeNode<E> child = children.get(index);
            if (predicate.test(child.getValue())) {
                return CompletableFuture.completedFuture(child);
            }

            CompletableFuture<TreeNode<E>> deep = child.findChildDeep(predicate);
            if (deep.isDone() && !deep.isCompletedExceptionally()) {
                TreeNode<E> found = deep.join();
                if (found != null) {
                    return CompletableFuture.completedFuture(found);
                }
                continue;
            }

            int next = index + 1;
            return deep.thenCompose(found -> found != null ? CompletableFuture.completedFuture(found) : findDeep(children, predicate, next));
        }
        return CompletableFuture.completedFuture(null);
    }

    public static void logBuildError(Logger log, @Nullable Throwable error) {
        if (error == null) {
            return;
        }

        Throwable cause = error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
        if (cause instanceof CancellationException || cause instanceof ProcessCanceledException) {
            return;
        }
        log.error(cause);
    }
}
