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
package org.consulo.idea.model;

import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.util.JDOMUtil;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;

/**
 * @author VISTALL
 * @since 9:49/16.06.13
 */
public class IdeaProjectModel extends HolderModel implements IdeaParseableModel {
  public IdeaProjectModel(File ideaProjectDir) {
    getInstance(IdeaProjectLibraryTableModel.class);
    getInstance(IdeaModuleTableModel.class);

    load(this, ideaProjectDir);
  }

  @NotNull
  public Document loadDocument(File file) throws JDOMException, IOException {
    return JDOMUtil.loadDocument(file);
  }

  @Override
  public void load(IdeaProjectModel ideaProjectModel, File ideaProjectDir) {
    for (Object o : myInstances.values()) {
      if(o instanceof IdeaParseableModel) {
        ((IdeaParseableModel)o).load(this, ideaProjectDir);
      }
    }
  }

  @Nullable
  public OrderRootType findOrderRootType(String libraryEntryName) {
    for (OrderRootType orderRootType : OrderRootType.getAllTypes()) {
      if (orderRootType.name().equals(libraryEntryName)) {
        return orderRootType;
      }
    }
    return null;
  }
}
