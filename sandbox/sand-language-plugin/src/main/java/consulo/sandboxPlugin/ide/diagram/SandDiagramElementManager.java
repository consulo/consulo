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
package consulo.sandboxPlugin.ide.diagram;

import consulo.dataContext.DataContext;
import consulo.diagram.AbstractDiagramElementManager;
import consulo.language.psi.PsiElement;
import consulo.sandboxPlugin.lang.psi.SandFile;
import consulo.ui.ex.SimpleColoredText;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.image.Image;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-09-30
 */
public class SandDiagramElementManager extends AbstractDiagramElementManager<String> {
    @Override
    public @Nullable String findInDataContext(DataContext context) {
        PsiElement element = context.getData(PsiElement.KEY);
        if (!(element instanceof SandFile sandFile)) {
            return null;
        }
        VirtualFile file = sandFile.getVirtualFile();
        return file == null ? null : file.getPath();
    }

    @Override
    public boolean isAcceptableAsNode(Object element) {
        return element instanceof String;
    }

    @Override
    public Object[] getNodeItems(String parent) {
        return SandDiagramMember.of(parent).toArray();
    }

    @Override
    public boolean canCollapse(String element) {
        return false;
    }

    @Override
    public boolean isContainerFor(String container, String element) {
        return false;
    }

    @Override
    public String getElementTitle(String element) {
        int index = element.lastIndexOf('/');
        return index == -1 ? element : element.substring(index + 1);
    }

    @Override
    public @Nullable SimpleColoredText getPresentableName(Object element) {
        return element instanceof SandDiagramMember member ? new SimpleColoredText(member.name(), SimpleTextAttributes.REGULAR_ATTRIBUTES) : null;
    }

    @Override
    public @Nullable SimpleColoredText getPresentableType(Object element) {
        return element instanceof SandDiagramMember member ? new SimpleColoredText(member.type(), SimpleTextAttributes.GRAYED_ATTRIBUTES) : null;
    }

    @Override
    public @Nullable Image getNodeElementIcon(Object element) {
        return element instanceof SandDiagramMember member ? member.icon() : null;
    }

    @Override
    public @Nullable String getElementDescription(String element) {
        return null;
    }
}
