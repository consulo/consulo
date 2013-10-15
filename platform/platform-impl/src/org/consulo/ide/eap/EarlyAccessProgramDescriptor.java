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
package org.consulo.ide.eap;

import com.intellij.openapi.extensions.ExtensionPointName;
import org.jetbrains.annotations.NotNull;

/**
 * @author VISTALL
 * @since 17:09/15.10.13
 */
public interface EarlyAccessProgramDescriptor {
  ExtensionPointName<EarlyAccessProgramDescriptor> EP_NAME = ExtensionPointName.create("com.intellij.eapDescriptor");

  @NotNull
  String getName();

  boolean getDefaultState();

  @NotNull
  String getDescription();
}
