/*
 * Copyright 2013-2016 consulo.io
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
package consulo.ide.impl;

import consulo.application.AllIcons;
import consulo.ide.impl.newProject.NewModuleBuilder;
import consulo.ide.impl.newProject.NewModuleBuilderProcessor;
import consulo.ide.impl.newProject.NewModuleContext;
import consulo.ide.impl.wizard.newModule.NewModuleWizardContext;
import consulo.ide.impl.wizard.newModule.NewModuleWizardContextBase;
import consulo.localize.LocalizeValue;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 05.06.14
 */
public class EmptyNewModuleBuilder implements NewModuleBuilder {
  @Override
  public void setupContext(@Nonnull NewModuleContext context) {
    context.add(LocalizeValue.localizeTODO("Empty"), AllIcons.FileTypes.Any_type, new NewModuleBuilderProcessor<NewModuleWizardContext>() {
      @Nonnull
      @Override
      public NewModuleWizardContext createContext(boolean isNewProject) {
        return new NewModuleWizardContextBase(isNewProject);
      }
    });
  }
}
