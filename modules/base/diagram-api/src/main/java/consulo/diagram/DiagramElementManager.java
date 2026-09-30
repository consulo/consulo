/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package consulo.diagram;

import consulo.dataContext.DataContext;
import consulo.ui.ex.SimpleColoredText;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

/**
 * @author Konstantin Bulenkov
 */
public interface DiagramElementManager<T> extends DiagramProviderHolder<T> {
    SimpleTextAttributes DEFAULT_TEXT_ATTR = SimpleTextAttributes.REGULAR_ATTRIBUTES;
    SimpleTextAttributes DEFAULT_TITLE_ATTR = SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES;

    @Nullable
    T findInDataContext(DataContext context);

    boolean isAcceptableAsNode(Object element);

    Object[] getNodeItems(T parent);

    @Nullable
    Image getNodeElementIcon(Object element);

    boolean canCollapse(T element);

    boolean isContainerFor(T container, T element);

    @Nullable
    String getElementTitle(T element);

    @Nullable
    SimpleColoredText getPresentableName(Object element);

    @Nullable
    SimpleColoredText getPresentableType(Object element);

    @Nullable
    String getElementDescription(T element);
}
