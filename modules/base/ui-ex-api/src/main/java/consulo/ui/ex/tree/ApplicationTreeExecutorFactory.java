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
package consulo.ui.ex.tree;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.disposer.Disposable;
import consulo.ui.TreeExecutor;

/**
 * The executors a tree over application data is created with - see
 * {@link consulo.ui.Tree#create(Object, consulo.ui.TreeModel, TreeExecutor, Disposable)}. A model that walks
 * PSI or indices must run under one of these: the task runs as a read action with write action priority, so a
 * write action cancels it rather than waiting behind it, and the cancelled task is restarted by itself.
 *
 * <p>There is no UI thread flavour here on purpose - a frontend runs the model on its own UI thread when the
 * tree is created without an executor, resolving the access of the component at each dispatch.
 *
 * @author VISTALL
 * @since 2026-08-29
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface ApplicationTreeExecutorFactory {
    /**
     * One background thread - the levels of the tree are built one after another, so the builds of two stored
     * paths over the same node cannot race each other.
     */
    TreeExecutor forBackgroundThreadWithReadAction(Disposable parent);

    /**
     * A pool of background threads - for a model whose nodes are independent enough to be built in parallel.
     */
    TreeExecutor forBackgroundPoolWithReadAction(Disposable parent);

    /**
     * One background thread without the read lock - for a model that is slow but does not touch application
     * data, walking a file system or a remote service.
     */
    TreeExecutor forBackgroundThreadWithoutReadAction(Disposable parent);
}
