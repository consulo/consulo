/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.fileEditor.structureView.tree;

import consulo.localize.LocalizeValue;

/**
 * @author Konstantin Bulenkov
 */
public class SorterUtil {
    private SorterUtil() {
    }

    public static LocalizeValue getStringPresentation(Object object) {
        return switch (object) {
            case SortableTreeElement sortableTreeElement -> LocalizeValue.of(sortableTreeElement.getAlphaSortKey());
            case TreeElement treeElement -> treeElement.getPresentation().getPresentableText();
            case Group group -> group.getPresentation().getPresentableText();
            default -> LocalizeValue.empty();
        };
    }
}
