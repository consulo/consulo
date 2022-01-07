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
package consulo.ui.dialog.impl;

import consulo.ui.Component;
import consulo.ui.dialog.Dialog;
import consulo.ui.dialog.DialogDescriptor;
import consulo.ui.dialog.DialogService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 14/12/2021
 */
public class UnifiedDialogServiceImpl implements DialogService {
  @Nonnull
  @Override
  public <V> Dialog<V> build(@Nullable Component parent, @Nonnull DialogDescriptor<V> descriptor) {
    return null;
  }
}
