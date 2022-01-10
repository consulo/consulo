/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package com.intellij.packageDependencies.ui;

import com.intellij.analysis.AnalysisScopeBundle;
import com.intellij.icons.AllIcons;
import com.intellij.ide.projectView.impl.ModuleGroup;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.psi.PsiFile;
import consulo.ui.image.Image;

import java.util.Set;

/**
 * User: anna
 * Date: 24-Jan-2006
 */
public class ModuleGroupNode extends PackageDependenciesNode {
  private final ModuleGroup myModuleGroup;

  public ModuleGroupNode(ModuleGroup moduleGroup, Project project) {
    super(project);
    myModuleGroup = moduleGroup;
  }

  @Override
  public void fillFiles(Set<PsiFile> set, boolean recursively) {
    super.fillFiles(set, recursively);
    int count = getChildCount();
    for (int i = 0; i < count; i++) {
      PackageDependenciesNode child = (PackageDependenciesNode)getChildAt(i);
      child.fillFiles(set, true);
    }
  }

  @Override
  public Image getIcon() {
    return AllIcons.Nodes.ModuleGroup;
  }

  @Override
  public String toString() {
    return myModuleGroup == null ? AnalysisScopeBundle.message("unknown.node.text") : myModuleGroup.toString();
  }

  public String getModuleGroupName() {
    return myModuleGroup.presentableText();
  }

  public ModuleGroup getModuleGroup() {
    return myModuleGroup;
  }

  @Override
  public boolean equals(Object o) {
    if (isEquals()){
      return super.equals(o);
    }
    if (this == o) return true;
    if (!(o instanceof ModuleGroupNode)) return false;

    final ModuleGroupNode moduleNode = (ModuleGroupNode)o;

    return Comparing.equal(myModuleGroup, moduleNode.myModuleGroup);
  }

  @Override
  public int hashCode() {
    return myModuleGroup == null ? 0 : myModuleGroup.hashCode();
  }
}
