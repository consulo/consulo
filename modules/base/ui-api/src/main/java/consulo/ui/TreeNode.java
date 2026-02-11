/*
 * Copyright 2013-2017 consulo.io
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
package consulo.ui;

import consulo.annotation.DeprecationInfo;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.function.BiConsumer;

/**
 * @author VISTALL
 * @since 2017-09-13
 */
public interface TreeNode<T> {
    void setRenderer(@Nonnull BiConsumer<T, TextItemPresentation> renderer);

    @Deprecated
    @DeprecationInfo("Use setRenderer")
    default void setRender(@Nonnull BiConsumer<T, TextItemPresentation> renderer) {
        setRenderer(renderer);
    }

    void setLeaf(boolean leaf);

    boolean isLeaf();

    /**
     * @return if rootValue is null and treeNode wrap it
     */
    @Nullable
    T getValue();
}
