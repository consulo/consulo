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
package consulo.ui;

import consulo.ui.internal.UIThreadTreeExecutor;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Runs the work of a {@link TreeModel} - building a level, computing the presentation of its nodes - on the
 * thread that work belongs to. A frontend never calls the model directly: everything the model computes goes
 * through the executor of its tree, and the frontend only touches its widget with the result.
 *
 * <p>The executor is chosen where the tree is created - see
 * {@link Tree#create(Object, TreeModel, TreeExecutor, consulo.disposer.Disposable)}. {@link #uiThread()} is
 * only right for a model that computes nothing - one backed by the application, walking PSI or indices, has
 * to be created with an executor that takes the read lock and yields to write actions.
 *
 * @author VISTALL
 * @since 2026-08-29
 */
@FunctionalInterface
public interface TreeExecutor {
    /**
     * The UI thread of the tree the task is for. It holds nothing, so the one instance serves every tree - the
     * access is resolved from the tree handed to {@link #execute}, which is what a frontend serving several
     * UIs requires.
     */
    static TreeExecutor uiThread() {
        return UIThreadTreeExecutor.INSTANCE;
    }

    /**
     * Executes the task on the thread this executor stands for and completes the returned future with its
     * result - on the same thread the task ran on. A task that could not run - the executor was disposed, or
     * the task kept being cancelled - completes the future exceptionally.
     *
     * @param tree the tree the task is being done for. An executor which dispatches to a UI resolves the
     *             access of that tree here, at every call, rather than holding one; the others ignore it.
     */
    <T> CompletableFuture<T> execute(Tree<?> tree, Supplier<T> task);
}
