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
package consulo.ide.impl.idea.openapi.roots.ui.configuration.libraryEditor;

import consulo.annotation.component.ExtensionImpl;
import consulo.content.base.DocumentationOrderRootType;
import consulo.content.bundle.Sdk;
import consulo.content.bundle.SdkType;
import consulo.content.library.ui.DocumentationUtil;
import consulo.fileChooser.FileChooserDescriptorFactory;
import consulo.ide.ui.OrderRootTypeUIFactory;
import consulo.ide.ui.SdkPathEditor;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.localize.ProjectLocalize;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.CustomShortcutSet;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.awt.ToolbarDecorator;
import consulo.ui.image.Image;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;

/**
 * @author anna
 * @since 2007-12-26
 */
@ExtensionImpl
public class DocumentationOrderRootTypeUIFactory implements OrderRootTypeUIFactory {
    
    @Override
    public String getOrderRootTypeId() {
        return "documentation";
    }

    
    @Override
    public SdkPathEditor createPathEditor(Sdk sdk) {
        return new DocumentationPathsEditor(sdk);
    }

    
    @Override
    public Image getIcon() {
        return PlatformIconGroup.filetypesText();
    }

    @Override
    public String getNodeText() {
        return ProjectLocalize.libraryJavadocsNode().get();
    }

    static class DocumentationPathsEditor extends SdkPathEditor {
        private final Sdk mySdk;

        public DocumentationPathsEditor(Sdk sdk) {
            super(
                ProjectLocalize.libraryJavadocsNode().get(),
                DocumentationOrderRootType.getInstance(),
                FileChooserDescriptorFactory.createMultipleJavaPathDescriptor(),
                sdk
            );
            mySdk = sdk;
        }

        @Override
        protected void addToolbarButtons(ToolbarDecorator toolbarDecorator) {
            AnAction specifyUrlButton =
                new DumbAwareAction(ProjectLocalize.sdkPathsSpecifyUrlButton(), LocalizeValue.empty(), PlatformIconGroup.nodesPpweb()) {
                    {
                        setShortcutSet(CustomShortcutSet.fromString("alt S"));
                    }

                    @RequiredUIAccess
                    @Override
                    public void actionPerformed(AnActionEvent e) {
                        onSpecifyUrlButtonClicked();
                    }

                    @RequiredUIAccess
                    @Override
                    public void update(AnActionEvent e) {
                        e.getPresentation().setEnabled(myEnabled);
                    }
                };
            toolbarDecorator.addExtraAction(specifyUrlButton);
        }

        private void onSpecifyUrlButtonClicked() {
            String defaultDocsUrl =
                mySdk == null ? "" : StringUtil.notNullize(((SdkType) mySdk.getSdkType()).getDefaultDocumentationUrl(mySdk), "");
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
