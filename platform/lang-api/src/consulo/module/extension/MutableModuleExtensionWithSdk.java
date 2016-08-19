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
package consulo.module.extension;

import com.intellij.openapi.projectRoots.Sdk;
import org.jetbrains.annotations.NotNull;

/**
 * @author VISTALL
 * @since 12:27/19.05.13
 */
public interface MutableModuleExtensionWithSdk<T extends ModuleExtensionWithSdk<T>> extends ModuleExtensionWithSdk<T>, MutableModuleExtension<T> {
  @Override
  @NotNull
  MutableModuleInheritableNamedPointer<Sdk> getInheritableSdk();
}
