/*
 * Copyright 2013 Consulo.org
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
package org.consulo.module.extension;

import com.intellij.openapi.module.Module;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author VISTALL
 * @since 10:32/19.05.13
 */
public interface ModuleExtensionProvider<ImmutableModel extends ModuleExtension, MutableModel extends MutableModuleExtension> {
   @Nullable
  Icon getIcon();

  @NotNull
  String getName();

  Class<ImmutableModel> getImmutableClass();

  ImmutableModel createImmutable(@NotNull String id, @NotNull Module module);

  MutableModel createMutable(@NotNull String id, @NotNull Module module, @NotNull ImmutableModel immutableModel);
}
