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
package consulo.ui.impl.tree;

import consulo.ui.TextItemPresentation;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * @author VISTALL
 * @since 2026-09-25
 */
public interface TreeWidget<E> {
    @Nullable
    UIAccess getUIAccess();

    default boolean isUIThread() {
        return UIAccess.isUIThread();
    }

    TextItemPresentation createPresentation();

    @RequiredUIAccess
    void showLoading(TreeNodeImpl<E> node);

    @RequiredUIAccess
    void hideLoading(TreeNodeImpl<E> node);

    @RequiredUIAccess
    void setChildren(TreeNodeImpl<E> node, List<TreeNodeImpl<E>> children);

    @RequiredUIAccess
    void setExpanded(TreeNodeImpl<E> node, boolean expanded);

    @RequiredUIAccess
    void setSelected(@Nullable TreeNodeImpl<E> node);

    @RequiredUIAccess
    void update(TreeNodeImpl<E> node);
}
