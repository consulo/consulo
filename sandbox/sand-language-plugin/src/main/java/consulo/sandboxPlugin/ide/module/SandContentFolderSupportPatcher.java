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
package consulo.sandboxPlugin.ide.module;

import consulo.annotation.component.ExtensionImpl;
import consulo.content.ContentFolderTypeProvider;
import consulo.content.base.WebResourcesFolderTypeProvider;
import consulo.language.content.ProductionContentFolderTypeProvider;
import consulo.language.content.ProductionResourceContentFolderTypeProvider;
import consulo.language.content.TestContentFolderTypeProvider;
import consulo.language.content.TestResourceContentFolderTypeProvider;
import consulo.module.content.layer.ContentFolderSupportPatcher;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.sandboxPlugin.ide.module.extension.SandModuleExtension;

import jakarta.annotation.Nonnull;
import java.util.Set;

/**
 * @author VISTALL
 * @since 31.03.14
 */
@ExtensionImpl
public class SandContentFolderSupportPatcher implements ContentFolderSupportPatcher {
  @Override
  public void patch(@Nonnull ModifiableRootModel model, @Nonnull Set<ContentFolderTypeProvider> set) {
    SandModuleExtension extension = model.getExtension(SandModuleExtension.class);
    if(extension != null) {
      set.add(ProductionContentFolderTypeProvider.getInstance());
      set.add(ProductionResourceContentFolderTypeProvider.getInstance());
      set.add(TestContentFolderTypeProvider.getInstance());
      set.add(TestResourceContentFolderTypeProvider.getInstance());
      set.add(WebResourcesFolderTypeProvider.getInstance());
    }
  }
}
