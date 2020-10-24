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
package consulo.ui.layout;

import consulo.annotation.DeprecationInfo;
import consulo.localize.LocalizeValue;
import consulo.ui.*;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.internal.UIInternal;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 15-Jun-16
 */
public interface LabeledLayout extends Layout {
  @Nonnull
  @Deprecated
  @DeprecationInfo("Use with LocalizeValue parameter")
  static LabeledLayout create(@Nonnull String label) {
    return create(LocalizeValue.of(label));
  }

  @Nonnull
  @RequiredUIAccess
  @Deprecated
  @DeprecationInfo("Use with LocalizeValue parameter")
  static LabeledLayout create(@Nonnull String label, @Nonnull Component component) {
    return create(LocalizeValue.of(label), component);
  }

  @Nonnull
  static LabeledLayout create(@Nonnull LocalizeValue label) {
    return UIInternal.get()._Layouts_labeled(label);
  }

  @Nonnull
  @RequiredUIAccess
  static LabeledLayout create(@Nonnull LocalizeValue label, @Nonnull Component component) {
    return UIInternal.get()._Layouts_labeled(label).set(component);
  }

  @Nonnull
  @RequiredUIAccess
  default LabeledLayout set(@Nonnull PseudoComponent component) {
    return set(component.getComponent());
  }

  @Nonnull
  @RequiredUIAccess
  LabeledLayout set(@Nonnull Component component);
}
