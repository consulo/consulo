/*
 * Copyright 2013-2016 consulo.io
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
package consulo.ui.internal;

import consulo.ui.ListBox;
import consulo.ui.model.ListModel;

/**
 * @author VISTALL
 * @since 16-Jun-16
 */
public class WGwtListBoxImpl<E> extends WGwtSingleListComponentImpl<E> implements ListBox<E>, VaadinWrapper {

  public WGwtListBoxImpl(ListModel<E> model) {
    super(model);
  }

  @Override
  protected boolean needRenderNullValue() {
    return false;
  }
}