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
package consulo.roots;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.roots.ModifiableRootModel;
import javax.annotation.Nonnull;

import java.util.Set;

/**
 * @author VISTALL
 * @since 31.03.14
 */
public interface ContentFolderSupportPatcher {
  ExtensionPointName<ContentFolderSupportPatcher> EP_NAME = ExtensionPointName.create("com.intellij.contentFolderSupportPatcher");

  void patch(@Nonnull ModifiableRootModel model, @Nonnull Set<ContentFolderTypeProvider> set);
}
