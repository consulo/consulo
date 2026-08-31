/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package consulo.ui.ex.util;

/**
 * @author Sergey.Malenkov
 * @deprecated legacy of the swing tree models, which could not be handed an executor - the executor of a tree
 * is chosen where the tree is created, see {@link consulo.ui.Tree#create(Object, consulo.ui.TreeModel, consulo.ui.TreeExecutor, consulo.disposer.Disposable)}
 */
@Deprecated
public interface InvokerSupplier {
  /**
   * @return preferable invoker to be used to access the supplier
   */
  Invoker getInvoker();
}
