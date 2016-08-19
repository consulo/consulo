/*
 * Copyright 2013-2014 must-be.org
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
import com.intellij.ui.components.JBLabel;
import consulo.ide.impl.NewModuleBuilder;
import consulo.ide.impl.NewModuleBuilderProcessor;
import consulo.ide.impl.NewModuleContext;
import consulo.ide.impl.UnzipNewModuleBuilderProcessor;
import consulo.roots.impl.ProductionContentFolderTypeProvider;
import consulo.sandboxPlugin.ide.module.extension.SandMutableModuleExtension;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author VISTALL
 * @since 05.06.14
 */
public class SandNewModuleBuilder implements NewModuleBuilder {
  @Override
  public void setupContext(@NotNull NewModuleContext context) {
    context.addItem("#SandGroup", "Sand", AllIcons.Nodes.Advice);
    // context.addItem("#JavaGroup", "Java", AllIcons.FileTypes.Java);
    // context.addItem("#JavaExample", "Hello World", AllIcons.RunConfigurations.Application);
    context.addItem("#SandExample", "Sand Example", AllIcons.Nodes.Static);
    context.addItem("#SandHello", "Sand Hello", AllIcons.Nodes.ProjectTab);

    context.setupItem(new String[]{"#SandGroup", "#SandHello"}, new UnzipNewModuleBuilderProcessor("/moduleTemplates/Hello.zip") {
      @NotNull
      @Override
      public JComponent createConfigurationPanel() {
        return new JBLabel("Hello World !!!");
      }

      @Override
      public void setupModule(@NotNull JComponent panel, @NotNull ContentEntry contentEntry, @NotNull ModifiableRootModel modifiableRootModel) {
        SandMutableModuleExtension extension = modifiableRootModel.getExtensionWithoutCheck(SandMutableModuleExtension.class);
        assert extension != null;
        extension.setEnabled(true);

        unzip(modifiableRootModel);

        contentEntry.addFolder(contentEntry.getUrl() + "/src", ProductionContentFolderTypeProvider.getInstance());
      }
    });

    context.setupItem(new String[]{"#SandGroup", "#SandExample"}, new NewModuleBuilderProcessor() {
      @NotNull
      @Override
      public JComponent createConfigurationPanel() {
        return new JBLabel("Sand Example!");
      }

      @Override
      public void setupModule(@NotNull JComponent panel, @NotNull ContentEntry contentEntry, @NotNull ModifiableRootModel modifiableRootModel) {
        SandMutableModuleExtension extension = modifiableRootModel.getExtensionWithoutCheck(SandMutableModuleExtension.class);
        assert extension != null;
        extension.setEnabled(true);
      }
    });

    /*context.setupItem(new String[]{"#JavaGroup", "#JavaExample"}, new NewModuleBuilderProcessor() {
      @NotNull
      @Override
      public JComponent createConfigurationPanel() {
        return new JBLabel("Java Example!");
      }

      @Override
      public void setupModule(@NotNull JComponent panel, @NotNull ContentEntry contentEntry, @NotNull ModifiableRootModel modifiableRootModel) {
      }
    }); */
  }
}
