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
package org.consulo.idea;

import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.util.io.FileUtilRt;
import org.consulo.idea.model.*;
import org.consulo.projectImport.ProjectModelProcessor;
import org.consulo.projectImport.model.ProjectModel;
import org.consulo.projectImport.model.library.LibraryModel;
import org.consulo.projectImport.model.library.OrderRootTypeModel;
import org.consulo.projectImport.model.module.ModuleModel;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Collection;
import java.util.Map;

/**
 * @author VISTALL
 * @since 17:42/19.06.13
 */
public class IdeaProjectModelProcessor extends ProjectModelProcessor<IdeaProjectModel> {

  @Override
  public void process(@NotNull IdeaProjectModel otherModel, @NotNull ProjectModel projectModel) {
    // convert libraries
    for(IdeaLibraryModel ideaLibraryModel : otherModel.getInstance(IdeaProjectLibraryTableModel.class).getLibraries()) {
      LibraryModel libraryModel = new LibraryModel(ideaLibraryModel.getName());
      for (Map.Entry<OrderRootType, Collection<String>> entry : ideaLibraryModel.getOrderRoots().entrySet()) {
        for (String url : entry.getValue()) {
          libraryModel.addUrl(fromIdeaOrderToConsuloOrderType(entry.getKey()), url);
        }
      }

      projectModel.getLibraryTable().addLibrary(libraryModel);
    }

    // convert modules
    for(IdeaModuleModel ideaModuleModel : otherModel.getInstance(IdeaModuleTableModel.class).getModules()) {
      File moduleFile = new File(ideaModuleModel.getFilePath());

      ModuleModel moduleModel = new ModuleModel(FileUtilRt.getNameWithoutExtension(moduleFile.getName()), moduleFile.getParent());

      projectModel.getModuleTable().addChild(moduleModel);
    }
  }

  @Override
  public boolean isAccepted(Object o) {
    return o instanceof IdeaProjectModel;
  }

  private static OrderRootTypeModel fromIdeaOrderToConsuloOrderType(@NotNull OrderRootType orderRootType) {
    if(orderRootType == OrderRootType.CLASSES) {
      return OrderRootTypeModel.BINARIES;
    }
    else if(orderRootType == OrderRootType.DOCUMENTATION) {
      return OrderRootTypeModel.DOCUMENTATION;
    }
    else if(orderRootType == OrderRootType.SOURCES) {
      return OrderRootTypeModel.SOURCES;
    }
    throw new UnsupportedOperationException(orderRootType.toString());
  }
}
