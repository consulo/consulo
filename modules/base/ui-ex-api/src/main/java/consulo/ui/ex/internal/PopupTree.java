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
package consulo.ui.ex.internal;

import consulo.ui.TreeNode;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.details.InputDetails;

import java.util.function.BiConsumer;
import java.util.function.Predicate;

/**
 * @author VISTALL
 * @since 2026-10-17
 */
public interface PopupTree<E> {
    @RequiredUIAccess
    void setSelectOnHover(boolean selectOnHover);

    @RequiredUIAccess
    void setSeparator(Predicate<E> separator);

    @RequiredUIAccess
    void setActivateListener(BiConsumer<TreeNode<E>, InputDetails> listener);

    @RequiredUIAccess
    void selectNext();

    @RequiredUIAccess
    void selectPrevious();

    boolean isExpanded(TreeNode<E> node);

    @RequiredUIAccess
    void setExpanded(TreeNode<E> node, boolean expanded);
}
