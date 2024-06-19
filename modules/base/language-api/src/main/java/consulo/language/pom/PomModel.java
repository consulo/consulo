/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.language.pom;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.disposer.Disposable;
import consulo.language.pom.event.PomModelListener;
import consulo.language.util.IncorrectOperationException;
import consulo.util.dataholder.UserDataHolder;

@ServiceAPI(ComponentScope.PROJECT)
public interface PomModel extends UserDataHolder {
  boolean isAllowPsiModification();

  <T extends PomModelAspect> T getModelAspect(Class<T> aClass);

  void addModelListener(PomModelListener listener, Disposable parentDisposable);

  void runTransaction(PomTransaction transaction) throws IncorrectOperationException;
}