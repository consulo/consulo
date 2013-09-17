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

import com.intellij.openapi.roots.ContentFolderType;
import com.intellij.openapi.roots.OrderRootType;
import org.consulo.idea.model.*;
import org.consulo.idea.model.orderEnties.*;

import java.util.List;

/**
 * @author VISTALL
 * @since 16:54/18.06.13
 */
public class ModuleLoaderTest extends ModuleLoaderTestCase {

  public void testProject1() {
    final IdeaProjectModel ideaProjectModel = getIdeaProjectModel();

    final IdeaProjectLibraryTableModel libraryTableModel = ideaProjectModel.getInstance(IdeaProjectLibraryTableModel.class);
    final List<IdeaLibraryModel> libraries = libraryTableModel.getLibraries();
    assertEquals(libraries.size(), 1);
    assertEquals(libraries.get(0).getName(), "concierge-1.0.0");
    assertEquals(libraries.get(0).getOrderRoots().get(OrderRootType.CLASSES).size(), 1);

    final IdeaModuleTableModel moduleTableModel = ideaProjectModel.getInstance(IdeaModuleTableModel.class);
    assertEquals(moduleTableModel.getModules().size(), 2);

    // test first module
    final IdeaModuleModel firstModule = moduleTableModel.getModules().get(0);
    assertEquals(firstModule.getModuleType(), "JAVA_MODULE");
    assertEquals(firstModule.getContentEntries().size(), 1);
    assertEquals(firstModule.getComponentAttributes().size(), 2);
    assertEquals(firstModule.getComponentAttributes().get("name"), "NewModuleRootManager");
    assertEquals(firstModule.getComponentAttributes().get("inherit-compiler-output"), "true");

    IdeaContentEntryModel contentEntryModel = firstModule.getContentEntries().get(0);

    assertEquals(contentEntryModel.getUrl(), "file://$MODULE_DIR$");
    assertEquals(contentEntryModel.getContentFolderTypes().size(), 2);
    assertEquals(contentEntryModel.getContentFolderTypes().get(ContentFolderType.PRODUCTION).size(), 1);
    assertEquals(contentEntryModel.getContentFolderTypes().get(ContentFolderType.PRODUCTION).iterator().next(), "file://$MODULE_DIR$/src");
    assertEquals(contentEntryModel.getContentFolderTypes().get(ContentFolderType.TEST).size(), 1);
    assertEquals(contentEntryModel.getContentFolderTypes().get(ContentFolderType.TEST).iterator().next(), "file://$MODULE_DIR$/testSrc");

    assertEquals(firstModule.getOrderEntries().size(), 5);
    assertEquals(firstModule.getOrderEntries().get(0).getClass(), InheritedOrderEntryModel.class);
    assertEquals(firstModule.getOrderEntries().get(1).getClass(), ModuleSourceOrderEntryModel.class);
    assertEquals(firstModule.getOrderEntries().get(2).getClass(), ModuleLibraryOrderEntryModel.class);
    assertEquals(firstModule.getOrderEntries().get(3).getClass(), ModuleLibraryOrderEntryModel.class);
    assertEquals(firstModule.getOrderEntries().get(4).getClass(), ProjectLibraryOrderEntryModel.class);

    final IdeaModuleModel secondModule = moduleTableModel.getModules().get(1);
    assertEquals(secondModule.getModuleType(), "JAVA_MODULE");
    assertEquals(secondModule.getContentEntries().size(), 1);
    assertEquals(secondModule.getComponentAttributes().size(), 3);
    assertEquals(secondModule.getComponentAttributes().get("name"), "NewModuleRootManager");
    assertEquals(secondModule.getComponentAttributes().get("inherit-compiler-output"), "true");
    assertEquals(secondModule.getComponentAttributes().get("LANGUAGE_LEVEL"), "JDK_1_7");

    contentEntryModel = secondModule.getContentEntries().get(0);

    assertEquals(contentEntryModel.getUrl(), "file://$MODULE_DIR$");
    assertEquals(contentEntryModel.getContentFolderTypes().size(), 1);
    assertEquals(contentEntryModel.getContentFolderTypes().get(ContentFolderType.PRODUCTION).size(), 1);
    assertEquals(contentEntryModel.getContentFolderTypes().get(ContentFolderType.PRODUCTION).iterator().next(), "file://$MODULE_DIR$/src");

    assertEquals(secondModule.getOrderEntries().size(), 4);
    assertEquals(secondModule.getOrderEntries().get(0).getClass(), JdkSourceOrderEntryModel.class);
    assertEquals(secondModule.getOrderEntries().get(1).getClass(), ModuleSourceOrderEntryModel.class);
    assertEquals(secondModule.getOrderEntries().get(2).getClass(), ModuleOrderEntryModel.class);
    assertEquals(secondModule.getOrderEntries().get(3).getClass(), ModuleLibraryOrderEntryModel.class);
  }
}
