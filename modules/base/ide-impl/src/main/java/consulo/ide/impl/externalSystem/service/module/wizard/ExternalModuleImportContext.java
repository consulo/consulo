/*
 * Copyright 2013-2017 consulo.io
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
package consulo.ide.impl.externalSystem.service.module.wizard;

import consulo.ide.impl.idea.openapi.externalSystem.service.settings.AbstractImportFromExternalSystemControl;
import consulo.project.Project;
import consulo.ide.moduleImport.ModuleImportContext;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 30-Jan-17
 */
public class ExternalModuleImportContext<C extends AbstractImportFromExternalSystemControl> extends ModuleImportContext {
  private final AbstractExternalModuleImportProvider<C> myImportProvider;

  public ExternalModuleImportContext(@Nullable Project project, AbstractExternalModuleImportProvider<C> importProvider) {
    super(project);
    myImportProvider = importProvider;
  }

  @Nonnull
  public AbstractExternalModuleImportProvider<C> getImportProvider() {
    return myImportProvider;
  }
}
