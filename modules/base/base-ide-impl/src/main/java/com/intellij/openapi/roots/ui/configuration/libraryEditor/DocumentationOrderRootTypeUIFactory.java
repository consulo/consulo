/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

/*
 * User: anna
 * Date: 26-Dec-2007
 */
package com.intellij.openapi.roots.ui.configuration.libraryEditor;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CustomShortcutSet;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkType;
import com.intellij.openapi.projectRoots.ui.SdkPathEditor;
import com.intellij.openapi.projectRoots.ui.Util;
import com.intellij.openapi.roots.ui.OrderRootTypeUIFactory;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.util.IconUtil;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.roots.types.DocumentationOrderRootType;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;

public class DocumentationOrderRootTypeUIFactory implements OrderRootTypeUIFactory {

  @Nonnull
  @Override
  public SdkPathEditor createPathEditor(Sdk sdk) {
    return new DocumentationPathsEditor(sdk);
  }

  @Nonnull
  @Override
  public Image getIcon() {
    return AllIcons.Nodes.JavaDocFolder;
  }

  @Nonnull
  @Override
  public String getNodeText() {
    return ProjectBundle.message("library.javadocs.node");
  }

  static class DocumentationPathsEditor extends SdkPathEditor {
    private final Sdk mySdk;

    public DocumentationPathsEditor(Sdk sdk) {
      super(ProjectBundle.message("library.javadocs.node"), DocumentationOrderRootType.getInstance(), FileChooserDescriptorFactory.createMultipleJavaPathDescriptor(), sdk);
      mySdk = sdk;
    }

    @Override
    protected void addToolbarButtons(ToolbarDecorator toolbarDecorator) {
      AnAction specifyUrlButton = new AnAction(ProjectBundle.message("sdk.paths.specify.url.button"), null, IconUtil.getAddLinkIcon()) {
        {
          setShortcutSet(CustomShortcutSet.fromString("alt S"));
        }
        @RequiredUIAccess
        @Override
        public void actionPerformed(@Nonnull AnActionEvent e) {
          onSpecifyUrlButtonClicked();
        }

        @RequiredUIAccess
        @Override
        public void update(@Nonnull AnActionEvent e) {
          e.getPresentation().setEnabled(myEnabled);
        }
      };
      toolbarDecorator.addExtraAction(specifyUrlButton);
    }

    private void onSpecifyUrlButtonClicked() {
      final String defaultDocsUrl = mySdk == null ? "" : StringUtil.notNullize(((SdkType)mySdk.getSdkType()).getDefaultDocumentationUrl(mySdk), "");
      VirtualFile virtualFile = Util.showSpecifyJavadocUrlDialog(myComponent, defaultDocsUrl);
      if (virtualFile != null) {
        addElement(virtualFile);
        setModified(true);
        requestDefaultFocus();
        setSelectedRoots(new Object[]{virtualFile});
      }
    }
  }
}
