/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.externalSystem.service;

import consulo.externalSystem.model.setting.ExternalSystemExecutionSettings;
import consulo.externalSystem.service.project.ExternalSystemProjectResolver;
import consulo.externalSystem.task.ExternalSystemTaskManager;
import jakarta.annotation.Nonnull;

import java.rmi.RemoteException;
import java.util.function.Supplier;

/**
 * @author Denis Zhdanov
 * @since 8/9/13 5:42 PM
 */
public class InProcessExternalSystemFacadeImpl<S extends ExternalSystemExecutionSettings> extends AbstractExternalSystemFacadeImpl<S> {

  public InProcessExternalSystemFacadeImpl(@Nonnull Supplier<ExternalSystemProjectResolver<S>> projectResolverClass,
                                           @Nonnull Supplier<ExternalSystemTaskManager<S>> buildManagerClass)
    throws IllegalAccessException, InstantiationException
  {
    super(projectResolverClass, buildManagerClass);
  }

  @Override
  protected <I extends RemoteExternalSystemService<S>, C extends I> I createService(@Nonnull Class<I> interfaceClass, @Nonnull C impl)
    throws ClassNotFoundException, IllegalAccessException, InstantiationException, RemoteException
  {
    return impl;
  }
}
