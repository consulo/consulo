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

import com.intellij.openapi.components.PathMacroMap;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.util.text.StringUtil;
import junit.framework.TestCase;
import org.consulo.idea.model.IdeaProjectModel;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;

/**
 * @author VISTALL
 * @since 17:03/18.06.13
 */
public abstract class ModuleLoaderTestCase extends TestCase {
  private IdeaProjectModel myIdeaProjectModel;

  @Override
  protected void runTest() throws Throwable {
    final File projectDir = new File("plugins/idea/testData/" + getName());
    final File ideaProjectDir = new File(projectDir, IdeaConstants.PROJECT_DIR);

    assertTrue(projectDir.exists());
    assertTrue(ideaProjectDir.exists());

    myIdeaProjectModel = new IdeaProjectModel(ideaProjectDir)
    {
      @Nullable
      @Override
      public OrderRootType findOrderRootType(String libraryEntryName) {
        if(libraryEntryName.equals("CLASSES")) {
          return OrderRootType.CLASSES;
        }
        else if(libraryEntryName.equals("SOURCES")) {
          return OrderRootType.SOURCES;
        }
        else if(libraryEntryName.equals("DOCUMENTATION")) {
          return OrderRootType.DOCUMENTATION;
        }
        return null;
      }

      @NotNull
      @Override
      public Document loadDocument(File file) throws JDOMException, IOException {
        Document document = super.loadDocument(file);
        PathMacroMap pathMacroMap = new PathMacroMap() {
          @Override
          public String substitute(String text, boolean caseSensitive) {
            return StringUtil.replace(text, "$PROJECT_DIR$", projectDir.getAbsolutePath(), !caseSensitive);
          }

          @Override
          public int hashCode() {
            return 1;
          }
        };
        pathMacroMap.substitute(document.getRootElement(), false);
        return document;
      }
    };

    super.runTest();
  }

  public IdeaProjectModel getIdeaProjectModel() {
    return myIdeaProjectModel;
  }
}
