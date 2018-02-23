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
import com.intellij.ui.components.JBLabel;
import consulo.ide.newProject.NewModuleBuilder;
import consulo.ide.newProject.NewModuleBuilderProcessor;
import consulo.ide.newProject.NewModuleContext;
import consulo.ide.impl.UnzipNewModuleBuilderProcessor;
import consulo.roots.impl.ProductionContentFolderTypeProvider;
import consulo.sandboxPlugin.ide.module.extension.SandMutableModuleExtension;
import javax.annotation.Nonnull;

import javax.swing.*;

/**
 * @author VISTALL
 * @since 05.06.14
 */
public class SandNewModuleBuilder implements NewModuleBuilder {
  @Override
  public void setupContext(@Nonnull NewModuleContext context) {
    NewModuleContext.Group group = context.createGroup("sand", "Sand");

    group.add("Sand Example", AllIcons.Nodes.Static, new UnzipNewModuleBuilderProcessor("/moduleTemplates/Hello.zip") {
      @Nonnull
      @Override
      public JComponent createConfigurationPanel() {
        return new JBLabel("Hello World !!!");
      }

      @Override
      public void setupModule(@Nonnull JComponent panel, @Nonnull ContentEntry contentEntry, @Nonnull ModifiableRootModel modifiableRootModel) {
        SandMutableModuleExtension extension = modifiableRootModel.getExtensionWithoutCheck(SandMutableModuleExtension.class);
        assert extension != null;
        extension.setEnabled(true);

        unzip(modifiableRootModel);

        contentEntry.addFolder(contentEntry.getUrl() + "/src", ProductionContentFolderTypeProvider.getInstance());
      }
    });

    group.add("Sand Hello", AllIcons.Nodes.ProjectTab, new NewModuleBuilderProcessor() {
      @Nonnull
      @Override
      public JComponent createConfigurationPanel() {
        return new JBLabel("Sand Example!");
      }

      @Override
      public void setupModule(@Nonnull JComponent panel, @Nonnull ContentEntry contentEntry, @Nonnull ModifiableRootModel modifiableRootModel) {
        SandMutableModuleExtension extension = modifiableRootModel.getExtensionWithoutCheck(SandMutableModuleExtension.class);
        assert extension != null;
        extension.setEnabled(true);
      }
    });
  }
}
