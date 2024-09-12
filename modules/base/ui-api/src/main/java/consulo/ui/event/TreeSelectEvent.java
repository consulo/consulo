/*
 * Copyright 2013-2024 consulo.io
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
package consulo.ui.event;

import consulo.ui.Tree;
import consulo.ui.TreeNode;
import consulo.ui.event.details.InputDetails;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2024-09-11
 */
public final class TreeSelectEvent<V> extends ComponentEvent<Tree<V>> {
    @Nullable
    private final TreeNode<V> myValue;

    public TreeSelectEvent(@Nonnull Tree<V> component, @Nullable TreeNode<V> value) {
        this(component, value, null);
    }

    public TreeSelectEvent(@Nonnull Tree<V> component, @Nullable TreeNode<V> value, @Nullable InputDetails inputDetails) {
        super(component, inputDetails);
        myValue = value;
    }

    @Nullable
    public TreeNode<V> getValue() {
        return myValue;
    }
}
