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
import consulo.annotation.access.RequiredReadAction;
import consulo.disposer.Disposable;
import consulo.ide.impl.UnzipNewModuleBuilderProcessor;
import consulo.ide.newProject.NewModuleBuilder;
import consulo.ide.newProject.NewModuleBuilderProcessor;
import consulo.ide.newProject.NewModuleContext;
import consulo.ide.newProject.node.NewModuleContextGroup;
import consulo.ide.newProject.ui.UnifiedProjectOrModuleNameStep;
import consulo.ide.wizard.newModule.NewModuleWizardContext;
import consulo.ide.wizard.newModule.NewModuleWizardContextBase;
import consulo.localize.LocalizeValue;
import consulo.roots.impl.ProductionContentFolderTypeProvider;
import consulo.sandboxPlugin.ide.module.extension.SandMutableModuleExtension;
import consulo.ui.ColorBox;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.ListBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;
import consulo.ui.style.StandardColors;
import consulo.ui.util.FormBuilder;
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
    NewModuleContextGroup group = context.addGroup("sand", LocalizeValue.localizeTODO("Sand"));

    group.add(LocalizeValue.localizeTODO("Sand Example"), AllIcons.Nodes.Static, new UnzipNewModuleBuilderProcessor<>("/moduleTemplates/Hello.zip") {
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

    group.add(LocalizeValue.localizeTODO("Empty"), AllIcons.FileTypes.Any_type, Integer.MAX_VALUE, new NewModuleBuilderProcessor<>() {
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
      }
    });

    NewModuleContextGroup subGroup = group.addGroup("sand-sub", LocalizeValue.localizeTODO("Sand Inner"), AllIcons.Nodes.Folder);

    subGroup.add(LocalizeValue.localizeTODO("Sand Hello"), AllIcons.Nodes.Project, new NewModuleBuilderProcessor<>() {
      @Nonnull
      @Override
      public NewModuleWizardContext createContext(boolean isNewProject) {
        return new NewModuleWizardContextBase(isNewProject);
      }

      @Override
      public void buildSteps(@Nonnull Consumer<WizardStep<NewModuleWizardContext>> consumer, @Nonnull NewModuleWizardContext context) {
        consumer.accept(new UnifiedProjectOrModuleNameStep<>(context) {
          @RequiredUIAccess
          @Override
          protected void extend(@Nonnull FormBuilder builder, Disposable uiDisposable) {
            builder.addLabeled(LocalizeValue.localizeTODO("Test"), ColorBox.create(StandardColors.LIGHT_RED));

            builder.addBottom(ListBox.create("Test1", "Test2"));
          }
        });

        consumer.accept(new WizardStep<>() {
          @RequiredUIAccess
          @Nonnull
          @Override
          public Component getComponent(@Nonnull NewModuleWizardContext context, @Nonnull Disposable uiDisposable) {
            return DockLayout.create().top(Label.create(LocalizeValue.localizeTODO("Hello world from sand plugin")));
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
