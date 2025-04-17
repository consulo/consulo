/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package consulo.language.editor.moveUpDown;

import consulo.annotation.access.RequiredReadAction;
import consulo.fileEditor.structureView.StructureViewModel;
import consulo.fileEditor.structureView.StructureViewTreeElement;
import consulo.fileEditor.structureView.TreeBasedStructureViewBuilder;
import consulo.fileEditor.structureView.tree.TreeElement;
import consulo.language.editor.structureView.PsiStructureViewFactory;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.util.collection.primitive.ints.IntList;
import consulo.util.collection.primitive.ints.IntLists;
import jakarta.annotation.Nonnull;

import java.util.Collection;
import java.util.HashSet;

public class MethodUpDownUtil {
    private MethodUpDownUtil() {
    }

    @RequiredReadAction
    public static int[] getNavigationOffsets(PsiFile file, int caretOffset) {
        for (MethodNavigationOffsetProvider provider
            : MethodNavigationOffsetProvider.EP_NAME.getExtensionList(file.getProject().getApplication())) {
            int[] offsets = provider.getMethodNavigationOffsets(file, caretOffset);
            if (offsets != null && offsets.length > 0) {
                return offsets;
            }
        }

        Collection<PsiElement> array = new HashSet<>();
        addNavigationElements(array, file);
        return offsetsFromElements(array);
    }

    public static int[] offsetsFromElements(Collection<PsiElement> array) {
        IntList offsets = IntLists.newArrayList(array.size());
        for (PsiElement element : array) {
            int offset = element.getTextOffset();
            assert offset >= 0 : element + " (" + element.getClass() + "); offset: " + offset;
            offsets.add(offset);
        }
        offsets.sort();
        return offsets.toArray();
    }

    @RequiredReadAction
    private static void addNavigationElements(Collection<PsiElement> array, PsiFile element) {
        if (PsiStructureViewFactory.createBuilderForFile(element) instanceof TreeBasedStructureViewBuilder builder) {
            StructureViewModel model = builder.createStructureViewModel(null);
            try {
                addStructureViewElements(model.getRoot(), array, element);
            }
            finally {
                model.dispose();
            }
        }
    }

    private static void addStructureViewElements(TreeElement parent, Collection<PsiElement> array, @Nonnull PsiFile file) {
        for (TreeElement treeElement : parent.getChildren()) {
            Object value = ((StructureViewTreeElement)treeElement).getValue();
            if (value instanceof PsiElement element) {
                if (array.contains(element) || !file.equals(element.getContainingFile())) {
                    continue;
                }
                array.add(element);
            }
            addStructureViewElements(treeElement, array, file);
        }
    }
}