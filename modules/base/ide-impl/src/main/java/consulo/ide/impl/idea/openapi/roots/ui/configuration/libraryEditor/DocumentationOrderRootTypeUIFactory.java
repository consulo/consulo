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
package consulo.ide.impl.idea.openapi.roots.ui.configuration.libraryEditor;

import consulo.annotation.component.ExtensionImpl;
import consulo.content.base.DocumentationOrderRootType;
import consulo.content.bundle.Sdk;
import consulo.content.bundle.SdkType;
import consulo.content.library.ui.DocumentationUtil;
import consulo.fileChooser.FileChooserDescriptorFactory;
import consulo.ide.impl.idea.util.IconUtil;
import consulo.ide.ui.OrderRootTypeUIFactory;
import consulo.ide.ui.SdkPathEditor;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.ProjectBundle;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.CustomShortcutSet;
import consulo.ui.ex.awt.ToolbarDecorator;
import consulo.ui.image.Image;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

@ExtensionImpl
public class DocumentationOrderRootTypeUIFactory implements OrderRootTypeUIFactory {

  @Nonnull
  @Override
  public String getOrderRootTypeId() {
    return "documentation";
  }

  @Nonnull
  @Override
  public SdkPathEditor createPathEditor(Sdk sdk) {
    return new DocumentationPathsEditor(sdk);
  }

  @Nonnull
  @Override
  public Image getIcon() {
    return PlatformIconGroup.filetypesText();
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
      VirtualFile virtualFile = DocumentationUtil.showSpecifyJavadocUrlDialog(myComponent, defaultDocsUrl);
      if (virtualFile != null) {
        addElement(virtualFile);
        setModified(true);
        requestDefaultFocus();
        setSelectedRoots(new Object[]{virtualFile});
      }
    }
  }
}
