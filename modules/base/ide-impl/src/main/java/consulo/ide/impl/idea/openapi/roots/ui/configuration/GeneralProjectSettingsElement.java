/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.roots.ui.configuration;

import consulo.compiler.util.ModuleCompilerUtil;
import consulo.component.util.graph.Graph;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.projectRoot.daemon.*;
import consulo.ide.setting.ProjectStructureSettingsUtil;
import consulo.ide.setting.ShowSettingsUtil;
import consulo.localize.LocalizeValue;
import consulo.module.content.layer.ModuleRootModel;
import consulo.project.Project;
import consulo.project.localize.ProjectLocalize;
import consulo.util.collection.Chunk;
import consulo.util.concurrent.AsyncResult;
import consulo.util.lang.StringUtil;
import org.jetbrains.annotations.NonNls;

import java.util.*;

/**
 * @author nik
 */
public class GeneralProjectSettingsElement extends ProjectStructureElement {
  @Override
  public String getPresentableName() {
    return "Project";
  }

  @Override
  public String getTypeName() {
    return "Project";
  }

  @Override
  public void check(Project project, ProjectStructureProblemsHolder problemsHolder) {
    ProjectStructureSettingsUtil util = (ProjectStructureSettingsUtil)ShowSettingsUtil.getInstance();

    ModulesConfiguratorImpl modulesConfigurator = (ModulesConfiguratorImpl)util.getModulesModel(project);

    final Graph<Chunk<ModuleRootModel>> graph = ModuleCompilerUtil.toChunkGraph(modulesConfigurator.createGraphGenerator());
    final Collection<Chunk<ModuleRootModel>> chunks = graph.getNodes();
    List<String> cycles = new ArrayList<>();
    for (Chunk<ModuleRootModel> chunk : chunks) {
      final Set<ModuleRootModel> modules = chunk.getNodes();
      List<String> names = new ArrayList<>();
      for (ModuleRootModel model : modules) {
        names.add(model.getModule().getName());
      }
      if (modules.size() > 1) {
        cycles.add(StringUtil.join(names, ", "));
      }
    }
    if (!cycles.isEmpty()) {
      final PlaceInProjectStructureBase place = new PlaceInProjectStructureBase(GeneralProjectSettingsElement::navigateToModules, this);
      final LocalizeValue message;
      final LocalizeValue description;
      if (cycles.size() > 1) {
        message = LocalizeValue.localizeTODO("Circular dependencies");
        @NonNls final String br = "<br>&nbsp;&nbsp;&nbsp;&nbsp;";
        StringBuilder cyclesString = new StringBuilder();
        for (int i = 0; i < cycles.size(); i++) {
          cyclesString.append(br).append(i + 1).append(". ").append(cycles.get(i));
        }
        description = ProjectLocalize.moduleCircularDependencyWarningDescription(cyclesString);
      }
      else {
        message = ProjectLocalize.moduleCircularDependencyWarningShort(cycles.get(0));
        description = null;
      }
      problemsHolder.registerProblem(new ProjectStructureProblemDescription(
        message.get(),
        description.get(),
        place,
        ProjectStructureProblemType.warning("module-circular-dependency"),
        Collections.<ConfigurationErrorQuickFix>emptyList()
      ));
    }
  }

  private static AsyncResult<Void> navigateToModules(Project project) {
    return ShowSettingsUtil.getInstance().showProjectStructureDialog(project, projectStructureSelector -> {
      projectStructureSelector.select(null, null, true);
    });
  }

  @Override
  public List<ProjectStructureElementUsage> getUsagesInElement() {
    return Collections.emptyList();
  }

  @Override
  public String getId() {
    return "project:general";
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof GeneralProjectSettingsElement;
  }

  @Override
  public int hashCode() {
    return 0;
  }
}
