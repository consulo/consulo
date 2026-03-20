/*
 * Copyright 2013-2025 consulo.io
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
package consulo.sandboxPlugin.ide;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.AllIcons;
import consulo.diagram.GraphBuilder;
import consulo.diagram.GraphBuilderFactory;
import consulo.diagram.GraphNode;
import consulo.diagram.GraphPositionStrategy;
import consulo.language.editor.diagram.LanguageGraphProvider;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.project.Project;
import consulo.sandboxPlugin.lang.psi.SandFile;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2025-09-02
 */
@ExtensionImpl
public class SandLanguageDiagramProvider implements LanguageGraphProvider<SandFile> {
    
    @Override
    public String getId() {
        return "sand";
    }

    
    @Override
    public String getName(SandFile element) {
        return element.getVirtualFile().getName();
    }

    @Override
    public boolean isSupported(PsiElement element) {
        return element instanceof SandFile;
    }

    @RequiredReadAction
    
    @Override
    public String getURL(SandFile element) {
        return element.getVirtualFile().getPath();
    }

    @RequiredReadAction
    @Override
    public @Nullable PsiElement restoreFromURL(Project project, String url) {
        VirtualFile file = LocalFileSystem.getInstance().findFileByPath(url);
        if (file == null || !file.isValid()) {
            return null;
        }

        PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
        if (psiFile instanceof SandFile sandFile) {
            return sandFile;
        }
        return null;
    }

    @RequiredReadAction
    
    @Override
    public GraphBuilder createBuilder(SandFile element) {
        GraphBuilderFactory graphBuilderFactory = GraphBuilderFactory.getInstance();

        GraphBuilder builder = graphBuilderFactory.createBuilder();

        GraphNode<?> testNode1 = builder.createNode("Test Node1", AllIcons.Nodes.Class, null, GraphPositionStrategy.CENTER);
        GraphNode<?> testNode2 = builder.createNode("Test Node2", AllIcons.Nodes.Class, null, GraphPositionStrategy.BOTTOM);

        testNode1.makeArrow(testNode2);

        return builder;
    }
}
