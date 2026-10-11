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
import consulo.content.base.SourcesOrderRootType;
import consulo.content.bundle.Sdk;
import consulo.content.bundle.SdkType;
import consulo.content.library.ui.DocumentationUtil;
import consulo.fileChooser.FileChooserDescriptorFactory;
import consulo.ide.ui.OrderRootTypeUIFactory;
import consulo.ide.ui.SdkPathEditor;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.localize.ProjectLocalize;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.toolbar.ToolbarDecoratorBuilder;
import consulo.ui.image.Image;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;

import java.util.List;

/**
 * @author anna
 * @since 2007-12-26
 */
@ExtensionImpl(id = DocumentationOrderRootType.ID, order = "after " + SourcesOrderRootType.ID)
public class DocumentationOrderRootTypeUIFactory implements OrderRootTypeUIFactory {
    
    @Override
    public String getOrderRootTypeId() {
        return DocumentationOrderRootType.ID;
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
    public LocalizeValue getNodeText() {
        return ProjectLocalize.libraryJavadocsNode();
    }

    static class DocumentationPathsEditor extends SdkPathEditor {
        private final Sdk mySdk;

        public DocumentationPathsEditor(Sdk sdk) {
            super(
                ProjectLocalize.libraryJavadocsNode(),
                DocumentationOrderRootType.ID,
                FileChooserDescriptorFactory.createMultipleJavaPathDescriptor(),
                sdk
            );
            mySdk = sdk;
        }

        @Override
        protected void addToolbarActions(ToolbarDecoratorBuilder<VirtualFile> builder) {
            builder.addExtraAction(new SpecifyUrlAction());
        }

        @RequiredUIAccess
        private void onSpecifyUrlButtonClicked() {
            String defaultDocsUrl = StringUtil.notNullize(((SdkType) mySdk.getSdkType()).getDefaultDocumentationUrl(mySdk), "");
            UIAccess uiAccess = UIAccess.current();
            DocumentationUtil.showSpecifyJavadocUrlDialogAsync(null, defaultDocsUrl).whenCompleteAsync((virtualFile, error) -> {
                if (error != null || virtualFile == null) {
                    return;
                }
                addPaths(virtualFile);
                setSelectedRoots(List.of(virtualFile));
             }, uiAccess);
        }

        private class SpecifyUrlAction extends DumbAwareAction implements AnActionWithSyncUpdate {
            private SpecifyUrlAction() {
                super(ProjectLocalize.sdkPathsSpecifyUrlButton(), LocalizeValue.empty(), PlatformIconGroup.nodesPpweb());
            }

            @RequiredUIAccess
            @Override
            public void actionPerformed(AnActionEvent e) {
                if (myEnabled) {
                    onSpecifyUrlButtonClicked();
                }
            }

            @Override
            public void update(AnActionEvent e) {
                e.getPresentation().setEnabled(myEnabled);
            }
        }
    }
}
