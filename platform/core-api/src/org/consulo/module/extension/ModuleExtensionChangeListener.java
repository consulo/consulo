/*
 * Copyright 2013 must-be.org
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

import org.jetbrains.annotations.NotNull;

import java.util.EventListener;

/**
 * @author VISTALL
 * @since 2:20/10.09.13
 */
public interface ModuleExtensionChangeListener extends EventListener {
  public class Adapter implements ModuleExtensionChangeListener {
    @Override
    public void beforeExtensionChanged(@NotNull ModuleExtension<?> oldExtension, @NotNull ModuleExtension<?> newExtension) {

    }

    @Override
    public void afterExtensionChanged(@NotNull ModuleExtension<?> oldExtension, @NotNull ModuleExtension<?> newExtension) {

    }
  }

  void beforeExtensionChanged(@NotNull ModuleExtension<?> oldExtension, @NotNull ModuleExtension<?> newExtension);

  void afterExtensionChanged(@NotNull ModuleExtension<?> oldExtension, @NotNull ModuleExtension<?> newExtension);
}
