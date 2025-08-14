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
package consulo.sandboxPlugin.lang.moduleImport;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.AllIcons;
import consulo.module.ModifiableModuleModel;
import consulo.module.Module;
import consulo.project.Project;
import consulo.annotation.access.RequiredReadAction;
import consulo.disposer.Disposable;
import consulo.ide.newModule.ui.UnifiedProjectOrModuleNameStep;
import consulo.localize.LocalizeValue;
import consulo.ide.moduleImport.ModuleImportContext;
import consulo.ide.moduleImport.ModuleImportProvider;
import consulo.ui.ColorBox;
import consulo.ui.Component;
import consulo.ui.ListBox;
import consulo.ui.TextBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.ui.layout.LabeledLayout;
import consulo.ui.style.StandardColors;
import consulo.ui.util.FormBuilder;
import consulo.ui.ex.wizard.WizardStep;

import jakarta.annotation.Nonnull;
import java.io.File;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2019-08-26
 */
@ExtensionImpl
public class Sand2ModuleImportProvider implements ModuleImportProvider<ModuleImportContext> {
  @Nonnull
  @Override
  public LocalizeValue getName() {
    return LocalizeValue.localizeTODO("sand2");
  }

  @Nonnull
  @Override
  public Image getIcon() {
    return AllIcons.Nodes.ClassInitializer;
  }

  @Override
  public boolean canImport(@Nonnull File fileOrDirectory) {
    return new File(fileOrDirectory, "sand2.txt").exists();
  }

  @Override
  public boolean isOnlyForNewImport() {
    return false;
  }

  @Override
  public void buildSteps(@Nonnull Consumer<WizardStep<ModuleImportContext>> consumer, @Nonnull ModuleImportContext context) {
    consumer.accept(new UnifiedProjectOrModuleNameStep<ModuleImportContext>(context) {
      @RequiredUIAccess
      @Override
      protected void extend(@Nonnull FormBuilder builder, Disposable uiDisposable) {
        builder.addLabeled(LocalizeValue.localizeTODO("Test"), ColorBox.create(StandardColors.RED));

        builder.addBottom(ListBox.create("Test1", "Test2"));
      }
    });

    consumer.accept(new WizardStep<ModuleImportContext>() {
      @RequiredUIAccess
      @Nonnull
      @Override
      public Component getComponent(@Nonnull ModuleImportContext context, @Nonnull Disposable uiDisposable) {
        return LabeledLayout.create(LocalizeValue.localizeTODO("Some Text"), TextBox.create("Test Value"));
      }
    });
  }

  @RequiredReadAction
  @Override
  public void process(@Nonnull ModuleImportContext context, @Nonnull Project project, @Nonnull ModifiableModuleModel model, @Nonnull Consumer<Module> newModuleConsumer) {

  }
}
