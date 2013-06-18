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

import com.intellij.openapi.util.io.FileUtilRt;
import lombok.SneakyThrows;
import org.jdom.Document;
import org.jdom.Element;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 9:49/16.06.13
 */
public class ProjectLibraryTableModel extends LibraryTableModel implements ParseableModel {
  private final List<LibraryModel> myLibraries = new ArrayList<LibraryModel>();

  @Override
  @SneakyThrows
  public void load(IdeaProjectModel ideaProjectModel, File ideaProjectDir) {
    File file = new File(ideaProjectDir, "libraries");
    if (!file.exists()) {
      return;
    }

    final FilenameFilter filter = new FilenameFilter() {
      @Override
      public boolean accept(File dir, String name) {
        return FileUtilRt.getExtension(name).equalsIgnoreCase("xml");
      }
    };

    for (File child : file.listFiles(filter)) {
      final Document document = ideaProjectModel.loadDocument(child);

      final Element rootElement = document.getRootElement();
      final String attributeValue = rootElement.getAttributeValue("name");
      if ("libraryTable".equals(attributeValue)) {
        final Element libraryElement = rootElement.getChild("library");
        if (libraryElement != null) {
          LibraryModel libraryModel = new LibraryModel();
          libraryModel.load(ideaProjectModel, rootElement);
          myLibraries.add(libraryModel);
        }
      }
    }
  }

  public List<LibraryModel> getLibraries() {
    return myLibraries;
  }
}
