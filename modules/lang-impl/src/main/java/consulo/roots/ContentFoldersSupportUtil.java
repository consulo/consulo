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

import com.intellij.openapi.roots.ModifiableRootModel;
import org.jetbrains.annotations.NotNull;
import consulo.roots.impl.ExcludedContentFolderTypeProvider;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author VISTALL
 * @since 13:25/07.11.13
 */
public class ContentFoldersSupportUtil {
  @NotNull
  public static Set<ContentFolderTypeProvider> getSupportedFolders(ModifiableRootModel moduleRootManager) {
    Set<ContentFolderTypeProvider> providers = new LinkedHashSet<ContentFolderTypeProvider>();
    for (ContentFolderSupportPatcher patcher : ContentFolderSupportPatcher.EP_NAME.getExtensions()) {
      patcher.patch(moduleRootManager, providers);
    }
    providers.add(ExcludedContentFolderTypeProvider.getInstance());
    return providers;
  }
}
