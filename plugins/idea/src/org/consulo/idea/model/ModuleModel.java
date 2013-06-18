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

import com.intellij.openapi.roots.ContentFolderType;
import lombok.SneakyThrows;
import org.consulo.idea.model.orderEnties.*;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.xpath.XPath;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author VISTALL
 * @since 9:57/16.06.13
 */
public class ModuleModel {
  private final List<ContentEntryModel> myContentEntries = new ArrayList<ContentEntryModel>();
  private final List<OrderEntryModel> myOrderEntries = new ArrayList<OrderEntryModel>();
  private String myModuleType;

  private final Map<String, String> myComponentAttributes = new HashMap<String, String>();

  @SneakyThrows
  public ModuleModel(IdeaProjectModel ideaProjectModel, String filepath) {
    final Document document = ideaProjectModel.loadDocument(new File(filepath));

    final Element rootElement = document.getRootElement();

    myModuleType = rootElement.getAttributeValue("type");

    XPath xpathExpression = XPath.newInstance("/module[@version='4']/component[@name='NewModuleRootManager']");
    final Element componentNode = (Element)xpathExpression.selectSingleNode(document);
    for(Attribute attribute : componentNode.getAttributes()) {
      myComponentAttributes.put(attribute.getName(), attribute.getValue());
    }

    for (Element element : componentNode.getChildren()) {
      final String name = element.getName();
      if ("content".equals(name)) {
        final String url = element.getAttributeValue("url");

        final ContentEntryModel contentEntryModel = new ContentEntryModel(url);
        myContentEntries.add(contentEntryModel);

        for (Element childOfContent : element.getChildren()) {
          final String nameChildOfContent = childOfContent.getName();
          if ("sourceFolder".equals(nameChildOfContent)) {
            String url2 = childOfContent.getAttributeValue("url");
            boolean isTestSource = Boolean.valueOf(childOfContent.getAttributeValue("isTestSource"));

            contentEntryModel.addFolder(url2, isTestSource ? ContentFolderType.TEST : ContentFolderType.SOURCE);
          }
        }
      }
      else if ("orderEntry".equals(name)) {
        String type = element.getAttributeValue("type");
        if ("module".equals(type)) {
          String moduleName = element.getAttributeValue("module-name");

          myOrderEntries.add(new ModuleOrderEntryModel(moduleName));
        }
        else if ("sourceFolder".equals(type)) {
          myOrderEntries.add(new ModuleSourceOrderEntryModel());
        }
        else if ("inheritedJdk".equals(type)) {
          myOrderEntries.add(new InheritedOrderEntryModel());
        }
        else if ("module-library".equals(type)) {
          LibraryModel libraryModel = new LibraryModel();
          libraryModel.load(ideaProjectModel, element);

          myOrderEntries.add(new ModuleLibraryOrderEntryModel(libraryModel));
        }
        else if ("jdk".equals(type)) {
          myOrderEntries.add(new JdkSourceOrderEntryModel(element.getAttributeValue("jdkName")));
        }
        else if ("library".equals(type)) {
          final String level = element.getAttributeValue("level");
          if ("project".equals(level)) {
            myOrderEntries.add(new ProjectLibraryOrderEntryModel(name));
          }
        }
      }
    }
  }

  public List<ContentEntryModel> getContentEntries() {
    return myContentEntries;
  }

  public List<OrderEntryModel> getOrderEntries() {
    return myOrderEntries;
  }

  public String getModuleType() {
    return myModuleType;
  }

  public Map<String, String> getComponentAttributes() {
    return myComponentAttributes;
  }
}
