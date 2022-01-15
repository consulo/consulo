/*
 * Copyright 2013-2019 consulo.io
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
package consulo.ide.newProject;

import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import consulo.annotation.access.RequiredReadAction;
import consulo.ide.newProject.ui.UnifiedProjectOrModuleNameStep;
import consulo.ide.wizard.newModule.NewModuleWizardContext;
import consulo.ui.wizard.WizardStep;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2019-08-20
 */
public interface NewModuleBuilderProcessor<C extends NewModuleWizardContext> {
  @Nonnull
  C createContext(boolean isNewProject);

  default void buildSteps(@Nonnull Consumer<WizardStep<C>> consumer, @Nonnull C context) {
    consumer.accept(new UnifiedProjectOrModuleNameStep<>(context));
  }

  @RequiredReadAction
  default void process(@Nonnull C context, @Nonnull ContentEntry contentEntry, @Nonnull ModifiableRootModel modifiableRootModel) {
  }
}
