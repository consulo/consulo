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
package org.consulo.idea.util.projectWizard;

import com.intellij.openapi.application.Result;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.components.PathMacroManager;
import com.intellij.openapi.module.ModifiableModuleModel;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.roots.ui.configuration.ModulesProvider;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.packaging.artifacts.ModifiableArtifactModel;
import com.intellij.projectImport.ProjectImportBuilder;
import org.consulo.idea.IdeaConstants;
import org.consulo.idea.IdeaIcons;
import org.consulo.idea.util.IdeaModuleTypeToModuleExtensionConverterEP;
import org.consulo.module.extension.ModuleExtension;
import org.consulo.module.extension.ModuleExtensionWithSdk;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.xpath.XPath;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author VISTALL
 * @since 18:49/14.06.13
 */
public class IdeaProjectImportBuilder extends ProjectImportBuilder<Object> {
  @NotNull
  @Override
  public String getName() {
    return "IntelliJ IDEA";
  }

  @Override
  public Icon getIcon() {
    return IdeaIcons.Idea;
  }

  @Override
  public List<Object> getList() {
    return null;
  }

  @Override
  public boolean isMarked(Object element) {
    return false;
  }

  @Override
  public void setList(List<Object> list) throws ConfigurationException {
  }

  @Override
  public void setOpenProjectSettingsAfter(boolean on) {
  }

  @Nullable
  @Override
  public List<Module> commit(Project project,
                             ModifiableModuleModel model,
                             ModulesProvider modulesProvider,
                             ModifiableArtifactModel artifactModel) {

    final String projectPath = project.getBasePath();
    File file = new File(projectPath, IdeaConstants.PROJECT_DIR);
    if (!file.exists()) {
      return null;
    }
    List<Module> modules = Collections.emptyList();
    try {
      modules = loadModules(file, model, project);
    }
    catch (JDOMException e) {
      e.printStackTrace();
    }
    catch (IOException e) {
      e.printStackTrace();
    }
    return modules;
  }

  private static List<Module> loadModules(File ideaDir, ModifiableModuleModel modifiableModuleModel, Project project) throws JDOMException, IOException {
    File modulesFile = new File(ideaDir, "modules.xml");
    if(!modulesFile.exists())  {
      return Collections.emptyList();
    }

    final Document document = JDOMUtil.loadDocument(modulesFile);

    PathMacroManager.getInstance(project).expandPaths(document.getRootElement());

    XPath xpathExpression = XPath.newInstance("/project[@version='4']/component[@name='ProjectModuleManager']/modules/*");

    final List list = xpathExpression.selectNodes(document);
    List<Module> modules = new ArrayList<Module>(list.size());
    for (Object o : list) {
      Element element = (Element)o;

      String filepath = element.getAttributeValue("filepath");
      if (filepath == null) {
        continue;
      }

      final Module module = loadModule(filepath, modifiableModuleModel, project);
      if(module == null) {
        continue;
      }
      modules.add(module);
    }

    return modules;
  }

  private static Module loadModule(String moduleFilePath, ModifiableModuleModel originalModel, Project project) throws JDOMException, IOException {
    final boolean fromProjectStructure = originalModel != null;

    File file = new File(moduleFilePath);
    if(!file.exists()) {
      return null;
    }

    final Document document = JDOMUtil.loadDocument(file);

    final ModifiableModuleModel newModel = fromProjectStructure ? originalModel : ModuleManager.getInstance(project).getModifiableModel();


    final Module module =
      newModel.newModule(FileUtil.getNameWithoutExtension(file), file.getParent());

    final Element rootElement = document.getRootElement();

    PathMacroManager.getInstance(module).expandPaths(document.getRootElement());

    String moduleType = rootElement.getAttributeValue("type");

    final ModifiableRootModel modifiableModel = ModuleRootManager.getInstance(module).getModifiableModel();

    for(IdeaModuleTypeToModuleExtensionConverterEP ep : IdeaModuleTypeToModuleExtensionConverterEP.EP_NAME.getExtensions()) {
      if(ep.getKey().equals(moduleType)) {
        ep.getInstance().convertTypeToExtension(modifiableModel);
        break;
      }
    }

    XPath xpathExpression = XPath.newInstance("/module[@version='4']/component[@name='NewModuleRootManager']/*");
    final List list = xpathExpression.selectNodes(document);
    for (Object o : list) {
      Element element = (Element)o;
      final String name = element.getName();
      if ("content".equals(name)) {
        final String url = element.getAttributeValue("url");

        final ContentEntry contentEntry = modifiableModel.addContentEntry(url);
        for(Element childOfContent : element.getChildren()) {
          final String nameChildOfContent = childOfContent.getName();
          if("sourceFolder".equals(nameChildOfContent)) {
            String url2 = childOfContent.getAttributeValue("url");
            boolean isTestSource = Boolean.valueOf(childOfContent.getAttributeValue("isTestSource"));
            contentEntry.addFolder(url2, isTestSource ? ContentFolderType.TEST : ContentFolderType.PRODUCTION);
          }
        }
      }
      else if("orderEntry".equals(name)) {
       String type = element.getAttributeValue("type");
        if("module".equals(type)) {
          String moduleName = element.getAttributeValue("module-name");

          modifiableModel.addInvalidModuleEntry(moduleName);
        }
        else if("module-library".equals(type)) {
          final Element libraryElement = element.getChild("library");

          final LibraryTable moduleLibraryTable = modifiableModel.getModuleLibraryTable();

          final Library library = moduleLibraryTable.createLibrary(libraryElement.getAttributeValue("name"));
          for(Element libraryEntry : libraryElement.getChildren()) {
            final String libraryEntryName = libraryEntry.getName();
            if("CLASSES".equals(libraryEntryName))  {
              parse(library, libraryEntry, OrderRootType.CLASSES);
            }
            else if("JAVADOC".equals(libraryEntryName)) {
              parse(library, libraryEntry, OrderRootType.DOCUMENTATION);
            }
            else if("SOURCES".equals(libraryEntryName)) {
              parse(library, libraryEntry, OrderRootType.SOURCES);
            }
          }
        }
      }
    }
    //TODO [VISTALL] facets converting

    for(ModuleExtension<?> moduleExtension : modifiableModel.getExtensions()) {
      if(moduleExtension instanceof ModuleExtensionWithSdk) {
        if(((ModuleExtensionWithSdk) moduleExtension).getInheritableSdk().isNull()) {
          continue;
        }
        modifiableModel.addModuleExtensionSdkEntry((ModuleExtensionWithSdk<?>)moduleExtension);
      }
    }

    new WriteAction<Object>()
    {
      @Override
      protected void run(Result<Object> result) throws Throwable {
        modifiableModel.commit();
        if(!fromProjectStructure) {
          newModel.commit();
        }
      }
    }.execute();



    return module;
  }

  private static void parse(Library library, Element element, OrderRootType orderRootType) {
    final Library.ModifiableModel modifiableModel = library.getModifiableModel();

    for(Element child : element.getChildren()) {
      final String name = child.getName();
      if("root".equals(name)) {
        final String url = child.getAttributeValue("url");
        modifiableModel.addRoot(url, orderRootType);
      }
    }
    modifiableModel.commit();
  }
}
