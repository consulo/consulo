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
package com.intellij.openapi.application.ex;

import com.intellij.openapi.components.ComponentConfig;
import com.intellij.openapi.components.impl.stores.IApplicationStore;
import org.jetbrains.annotations.NotNull;

/**
 * @author VISTALL
 * @since 12:05/12.08.13
 */
public interface ApplicationEx2 extends ApplicationEx {
  void initComponents();

  @NotNull
  ComponentConfig[] getComponentConfigurations();

  IApplicationStore getStateStore();
}
