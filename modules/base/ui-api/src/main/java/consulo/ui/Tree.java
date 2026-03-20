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
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 12-Sep-17
 */
public interface Tree<E> extends Component {
  static <E> Tree<E> create(TreeModel<E> model, Disposable disposable) {
    return create(null, model, disposable);
  }

  static <E> Tree<E> create(@Nullable E rootValue, TreeModel<E> model, Disposable disposable) {
    return UIInternal.get()._Components_tree(rootValue, model, disposable);
  }

  @Nullable TreeNode<E> getSelectedNode();

  void expand(TreeNode<E> node);

  @SuppressWarnings("unchecked")
  default Disposable addSelectListener(ComponentEventListener<Tree<E>, TreeSelectEvent<E>> listener) {
    return addListener((Class) TreeSelectEvent.class, listener);
  }
}
