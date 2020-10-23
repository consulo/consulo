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
package consulo.roots.ui.configuration.classpath;

import com.intellij.icons.AllIcons;
import com.intellij.ide.util.ChooseElementsDialog;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.ui.configuration.classpath.ClasspathPanel;
import com.intellij.openapi.roots.ui.configuration.libraries.LibraryPresentationManager;
import com.intellij.openapi.roots.ui.configuration.projectRoot.StructureConfigurableContext;
import consulo.localize.LocalizeValue;
import consulo.roots.ModuleRootLayer;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.util.concurrent.AsyncResult;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * @author VISTALL
 * @since 27.09.14
 */
public class ProjectLibraryActionProvider implements AddModuleDependencyActionProvider<List<Library>, ProjectLibraryContext> {
  private static class ChooseLibrariesDialogImpl extends ChooseElementsDialog<Library> {
    private final StructureConfigurableContext myContext;

    protected ChooseLibrariesDialogImpl(StructureConfigurableContext context, List<Library> libraries) {
      super(context.getProject(), libraries, "Add Library Dependency", "Choose libraries for adding as dependency");
      myContext = context;
    }

    @Override
    protected String getItemText(Library item) {
      return item.getName();
    }

    @Nullable
    @Override
    protected Image getItemIcon(Library item) {
      Image customIcon = LibraryPresentationManager.getInstance().getCustomIcon(item, myContext);
      if(customIcon != null) {
        return customIcon;
      }

      return AllIcons.Nodes.PpLib;
    }
  }

  @Nonnull
  @Override
  public LocalizeValue getActionName(@Nonnull ModuleRootLayer layer) {
    return LocalizeValue.of("Library");
  }

  @Nonnull
  @Override
  public Image getIcon(@Nonnull ModuleRootLayer layer) {
    return AllIcons.Nodes.PpLib;
  }

  @Override
  public ProjectLibraryContext createContext(@Nonnull ClasspathPanel classpathPanel, @Nonnull StructureConfigurableContext context) {
    return new ProjectLibraryContext(classpathPanel, context);
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public AsyncResult<List<Library>> invoke(@Nonnull ProjectLibraryContext context) {
    return new ChooseLibrariesDialogImpl(context.getStructureContext(), context.getItems()).showAsync2();
  }
}
