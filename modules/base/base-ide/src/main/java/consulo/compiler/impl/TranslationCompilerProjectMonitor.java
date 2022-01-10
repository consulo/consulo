/*
 * Copyright 2013-2019 consulo.io
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
package consulo.compiler.impl;

import com.intellij.openapi.compiler.CompilerPaths;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Couple;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import consulo.annotation.access.RequiredReadAction;
import consulo.compiler.ModuleCompilerPathsManager;
import consulo.logging.Logger;
import consulo.roots.impl.ProductionContentFolderTypeProvider;
import consulo.roots.impl.TestContentFolderTypeProvider;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author VISTALL
 * @since 2019-12-08
 */
@Singleton
public class TranslationCompilerProjectMonitor {
  @Nonnull
  public static TranslationCompilerProjectMonitor getInstance(@Nonnull Project project) {
    return ServiceManager.getService(project, TranslationCompilerProjectMonitor.class);
  }

  private static final Logger LOG = Logger.getInstance(TranslationCompilerProjectMonitor.class);

  private final Project myProject;

  @Inject
  public TranslationCompilerProjectMonitor(Project project) {
    myProject = project;
  }

  @RequiredReadAction
  public void updateCompileOutputInfoFile() {
    Map<String, Couple<String>> map = buildOutputRootsLayout();

    Element root = new Element("list");
    for (Map.Entry<String, Couple<String>> entry : map.entrySet()) {
      Element module = new Element("module");
      root.addContent(module);

      module.setAttribute("name", entry.getKey());

      String first = entry.getValue().getFirst();
      if (first != null) module.setAttribute("output-url", first);

      String second = entry.getValue().getSecond();
      if (second != null) module.setAttribute("test-output-url", second);
    }

    try {
      JDOMUtil.writeDocument(new Document(root), getOutputUrlsFile(), "\n");
    }
    catch (IOException e) {
      LOG.error(e);
    }
  }

  @Nonnull
  public Map<String, Couple<String>> getLastOutputRootsLayout() {
    File file = getOutputUrlsFile();

    Map<String, Couple<String>> map = new HashMap<>();
    if (file.exists()) {
      try {
        Element root = JDOMUtil.load(file);

        for (Element module : root.getChildren()) {
          String name = module.getAttributeValue("name");
          String outputUrl = module.getAttributeValue("output-url");
          String testOutputUrl = module.getAttributeValue("test-output-url");

          map.put(name, Couple.of(outputUrl, testOutputUrl));
        }
      }
      catch (IOException | JDOMException e) {
        LOG.error(e);
      }
    }

    return map;
  }

  public void removeCompileOutputInfoFile() {
    File outputUrlsFile = getOutputUrlsFile();
    FileUtil.delete(outputUrlsFile);
  }

  @RequiredReadAction
  public Map<String, Couple<String>> buildOutputRootsLayout() {
    Map<String, Couple<String>> map = new LinkedHashMap<>();

    for (Module module : ModuleManager.getInstance(myProject).getModules()) {
      ModuleCompilerPathsManager moduleCompilerPathsManager = ModuleCompilerPathsManager.getInstance(module);

      final VirtualFile output = moduleCompilerPathsManager.getCompilerOutput(ProductionContentFolderTypeProvider.getInstance());
      final String outputUrl = output == null ? null : output.getUrl();
      final VirtualFile testsOutput = moduleCompilerPathsManager.getCompilerOutput(TestContentFolderTypeProvider.getInstance());
      final String testoutUrl = testsOutput == null ? null : testsOutput.getUrl();
      map.put(module.getName(), Couple.of(outputUrl, testoutUrl));
    }

    return map;
  }

  @Nonnull
  private File getOutputUrlsFile() {
    File dir = CompilerPaths.getCompilerSystemDirectory(myProject);
    return new File(dir, "outputs.xml");
  }
}
