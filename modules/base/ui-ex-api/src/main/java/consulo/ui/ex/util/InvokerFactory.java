/*
 * Copyright 2013-2022 consulo.io
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

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.disposer.Disposable;
import consulo.ui.UIAccess;

/**
 * @author VISTALL
 * @since 24-Feb-22
 * @deprecated use {@link consulo.ui.ex.tree.ApplicationTreeExecutorFactory}
 */
@Deprecated
@ServiceAPI(ComponentScope.APPLICATION)
public interface InvokerFactory {

  static InvokerFactory getInstance() {
    return Application.get().getInstance(InvokerFactory.class);
  }

  /**
   * The swing event dispatch thread - the foreground of {@code AsyncTreeModel}, which is the only tree stack
   * this factory still serves. It is one thread for the whole application, so no {@link UIAccess} is taken
   * or held.
   */
  Invoker forEventDispatchThread(Disposable parent);


  Invoker forBackgroundPoolWithReadAction(Disposable parent);

  
  Invoker forBackgroundThreadWithReadAction(Disposable parent);

  
  Invoker forBackgroundThreadWithoutReadAction(Disposable parent);
}
