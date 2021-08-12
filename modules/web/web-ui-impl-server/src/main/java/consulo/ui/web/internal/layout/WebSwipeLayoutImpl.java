/*
 * Copyright 2013-2021 consulo.io
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
package consulo.ui.web.internal.layout;

import consulo.ui.layout.Layout;
import consulo.ui.layout.SwipeLayout;
import consulo.ui.web.internal.base.WebUnsupportedComponent;

import javax.annotation.Nonnull;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 01/08/2021
 */
public class WebSwipeLayoutImpl extends WebUnsupportedComponent implements SwipeLayout {
  @Nonnull
  @Override
  public SwipeLayout register(@Nonnull String id, @Nonnull Supplier<Layout> layoutSupplier) {
    return this;
  }

  @Nonnull
  @Override
  public Layout swipeLeftTo(@Nonnull String id) {
    return this;
  }

  @Nonnull
  @Override
  public Layout swipeRightTo(@Nonnull String id) {
    return this;
  }
}
