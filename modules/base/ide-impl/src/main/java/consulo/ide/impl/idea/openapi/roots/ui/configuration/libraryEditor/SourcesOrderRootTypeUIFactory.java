/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.roots.ui.configuration.libraryEditor;

import consulo.annotation.component.ExtensionImpl;
import consulo.content.base.SourcesOrderRootType;
import consulo.content.bundle.Sdk;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.ide.ui.OrderRootTypeUIFactory;
import consulo.ide.ui.SdkPathEditor;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.localize.ProjectLocalize;
import consulo.ui.image.Image;

/**
 * @author anna
 * @since 2007-12-26
 */
@ExtensionImpl
public class SourcesOrderRootTypeUIFactory implements OrderRootTypeUIFactory {
  @Override
  public String getOrderRootTypeId() {
    return "sources";
  }

  @Override
  public SdkPathEditor createPathEditor(Sdk sdk) {
    return new SdkPathEditor(
      ProjectLocalize.librarySourcesNode().get(),
      SourcesOrderRootType.getInstance(),
      new FileChooserDescriptor(true, true, true, false, true, true),
      sdk
    );
  }

  @Override
  public Image getIcon() {
    return PlatformIconGroup.modulesSourceroot();
  }

  @Override
  public String getNodeText() {
    return ProjectLocalize.librarySourcesNode().get();
  }
}
