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
package consulo.sandboxPlugin.ide.module;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import consulo.annotations.RequiredReadAction;
import consulo.ide.impl.UnzipNewModuleBuilderProcessor;
import consulo.ide.newProject.NewModuleBuilder;
import consulo.ide.newProject.NewModuleBuilderProcessor;
import consulo.ide.newProject.NewModuleContext;
import consulo.ide.wizard.newModule.NewModuleWizardContext;
import consulo.ide.wizard.newModule.NewModuleWizardContextBase;
import consulo.roots.impl.ProductionContentFolderTypeProvider;
import consulo.sandboxPlugin.ide.module.extension.SandMutableModuleExtension;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.RequiredUIAccess;
import consulo.ui.layout.DockLayout;
import consulo.ui.wizard.WizardStep;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 05.06.14
 */
public class SandNewModuleBuilder implements NewModuleBuilder {
  @Override
  public void setupContext(@Nonnull NewModuleContext context) {
    NewModuleContext.Group group = context.createGroup("sand", "Sand");

    group.add("Sand Example", AllIcons.Nodes.Static, new UnzipNewModuleBuilderProcessor<NewModuleWizardContext>("/moduleTemplates/Hello.zip") {
      @Nonnull
      @Override
      public NewModuleWizardContext createContext(boolean isNewProject) {
        return new NewModuleWizardContextBase(isNewProject);
      }

      @RequiredReadAction
      @Override
      public void process(@Nonnull NewModuleWizardContext context, @Nonnull ContentEntry contentEntry, @Nonnull ModifiableRootModel modifiableRootModel) {
        SandMutableModuleExtension extension = modifiableRootModel.getExtensionWithoutCheck(SandMutableModuleExtension.class);
        assert extension != null;
        extension.setEnabled(true);

        unzip(modifiableRootModel);

        contentEntry.addFolder(contentEntry.getUrl() + "/src", ProductionContentFolderTypeProvider.getInstance());
      }
    });

    group.add("Sand Hello", AllIcons.Nodes.ProjectTab, new NewModuleBuilderProcessor<NewModuleWizardContext>() {
      @Nonnull
      @Override
      public NewModuleWizardContext createContext(boolean isNewProject) {
        return new NewModuleWizardContextBase(isNewProject);
      }

      @Override
      public void buildSteps(@Nonnull Consumer<WizardStep<NewModuleWizardContext>> consumer, @Nonnull NewModuleWizardContext context) {
        NewModuleBuilderProcessor.super.buildSteps(consumer, context);

        consumer.accept(new WizardStep<NewModuleWizardContext>() {
          @RequiredUIAccess
          @Nonnull
          @Override
          public Component getComponent() {
            return DockLayout.create().top(Label.create("Hello world from sand plugin"));
          }
        });
      }

      @RequiredReadAction
      @Override
      public void process(@Nonnull NewModuleWizardContext context, @Nonnull ContentEntry contentEntry, @Nonnull ModifiableRootModel modifiableRootModel) {
        SandMutableModuleExtension extension = modifiableRootModel.getExtensionWithoutCheck(SandMutableModuleExtension.class);
        assert extension != null;
        extension.setEnabled(true);
      }
    });
  }
}
