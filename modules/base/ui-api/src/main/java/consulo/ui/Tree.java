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

import consulo.disposer.Disposable;
import consulo.ui.event.ComponentEventListener;
import consulo.ui.event.TreeSelectEvent;
import consulo.ui.internal.UIInternal;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 12-Sep-17
 */
public interface Tree<E> extends Component {
  @Nonnull
  static <E> Tree<E> create(@Nonnull TreeModel<E> model, @Nonnull Disposable disposable) {
    return create(null, model, disposable);
  }

  @Nonnull
  static <E> Tree<E> create(@Nullable E rootValue, @Nonnull TreeModel<E> model, @Nonnull Disposable disposable) {
    return UIInternal.get()._Components_tree(rootValue, model, disposable);
  }

  @Nullable
  TreeNode<E> getSelectedNode();

  void expand(@Nonnull TreeNode<E> node);

  @Nonnull
  @SuppressWarnings("unchecked")
  default Disposable addSelectListener(@Nonnull ComponentEventListener<Tree<E>, TreeSelectEvent<E>> listener) {
    return addListener((Class) TreeSelectEvent.class, listener);
  }
}
