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
package consulo.sandboxPlugin.ide.library;

import consulo.annotation.component.ExtensionImpl;
import consulo.content.library.DummyLibraryProperties;
import consulo.application.AllIcons;
import consulo.content.library.NewLibraryConfiguration;
import consulo.content.library.PersistentLibraryKind;
import consulo.content.library.ui.LibraryEditorComponent;
import consulo.content.library.ui.LibraryPropertiesEditor;
import consulo.module.content.layer.ModuleRootLayer;
import consulo.module.content.library.ModuleAwareLibraryType;
import consulo.project.Project;
import consulo.sandboxPlugin.ide.module.extension.SandModuleExtension;
import consulo.ui.image.Image;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Inject;

import org.jspecify.annotations.Nullable;
import javax.swing.*;

/**
 * @author VISTALL
 * @since 20.03.14
 */
@ExtensionImpl
public class SandLibraryType extends ModuleAwareLibraryType<DummyLibraryProperties> {
  @Inject
  protected SandLibraryType() {
    super(new PersistentLibraryKind<>("sand") {
      
      @Override
      public DummyLibraryProperties createDefaultProperties() {
        return new DummyLibraryProperties();
      }
    });
  }

  @Override
  public @Nullable String getCreateActionName() {
    return "test";
  }

  @Override
  public @Nullable LibraryPropertiesEditor createPropertiesEditor(LibraryEditorComponent<DummyLibraryProperties> editorComponent) {
    return null;
  }

  @Override
  public @Nullable Image getIcon() {
    return AllIcons.Nodes.Static;
  }

  @Override
  public boolean isAvailable(ModuleRootLayer model) {
    return model.getExtension(SandModuleExtension.class) != null;
  }
}
