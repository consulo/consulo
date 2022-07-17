/*
 * Copyright 2000-2010 JetBrains s.r.o.
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

package consulo.language.editor.impl.internal.generation;

import consulo.annotation.component.ActionImpl;
import consulo.application.Application;
import consulo.language.editor.generation.DelegateMethodHandler;
import jakarta.inject.Inject;

@ActionImpl(id = "DelegateMethods")
public class DelegateMethodsAction extends MethodsBasedAction<DelegateMethodHandler> {
  @Inject
  public DelegateMethodsAction(Application application) {
    super(application, DelegateMethodHandler.class, DelegateMethodHandler.KEY);
  }
}