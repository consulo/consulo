/*
 * Copyright 2013-2026 consulo.io
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
package consulo.ide.impl.idea.ide.scratch;

import consulo.annotation.component.ExtensionImpl;
import consulo.dataContext.DataSink;
import consulo.dataContext.DataSnapshot;
import consulo.dataContext.UiDataRule;
import consulo.language.editor.LangDataKeys;
import consulo.language.editor.PlatformDataKeys;
import consulo.language.psi.PsiManager;
import consulo.project.Project;
import consulo.project.ui.view.tree.AbstractTreeNode;
import consulo.util.collection.JBIterable;
import consulo.virtualFileSystem.VirtualFile;

/**
 * @author VISTALL
 * @since 2026-03-06
 */
@ExtensionImpl
public class ScratchTreeUiDataRule implements UiDataRule {
    @Override
    public void uiDataSnapshot(DataSink sink, DataSnapshot snapshot) {
        AbstractTreeNode<?> node = JBIterable.of(snapshot.get(PlatformDataKeys.SELECTED_ITEMS))
            .filter(AbstractTreeNode.class)
            .single();

        if (node instanceof ScratchTreeStructureProvider.MyRootNode selection) {
            sink.lazy(LangDataKeys.PASTE_TARGET_PSI_ELEMENT, () -> {
                VirtualFile file = selection.getVirtualFile();
                Project project = selection.getProject();
                return file == null || project == null ? null : PsiManager.getInstance(project).findDirectory(file);
            });
        }
    }
}
