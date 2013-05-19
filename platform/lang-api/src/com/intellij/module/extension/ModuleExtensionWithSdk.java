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
package com.intellij.module.extension;

import com.intellij.openapi.module.extension.ModuleExtension;
import com.intellij.openapi.projectRoots.Sdk;

/**
 * @author VISTALL
 * @since 23:26/18.05.13
 */
public interface ModuleExtensionWithSdk extends ModuleExtension {
  Sdk getSdk();
}
