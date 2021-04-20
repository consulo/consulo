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
package com.intellij.openapi.roots.ui.configuration.artifacts.sourceItems.actions;

import com.intellij.openapi.roots.ui.configuration.artifacts.ArtifactsStructureConfigurableContext;
import com.intellij.openapi.roots.ui.configuration.artifacts.actions.ArtifactEditorFindUsagesActionBase;
import com.intellij.openapi.roots.ui.configuration.artifacts.nodes.ArtifactsTreeNode;
import com.intellij.openapi.roots.ui.configuration.artifacts.sourceItems.SourceItemNode;
import com.intellij.openapi.roots.ui.configuration.artifacts.sourceItems.SourceItemsTree;
import com.intellij.openapi.roots.ui.configuration.projectRoot.daemon.ProjectStructureElement;

import java.util.List;

/**
 * @author nik
 */
public class SourceItemFindUsagesAction extends ArtifactEditorFindUsagesActionBase {
  private final SourceItemsTree myTree;

  public SourceItemFindUsagesAction(SourceItemsTree tree, ArtifactsStructureConfigurableContext artifactContext) {
    super(tree, artifactContext);
    myTree = tree;
  }

  @Override
  protected ProjectStructureElement getSelectedElement() {
    final List<SourceItemNode> nodes = myTree.getSelectedSourceItemNodes();
    if (nodes.size() != 1) return null;
    ArtifactsTreeNode node = nodes.get(0);
    if (!(node instanceof SourceItemNode)) {
      return null;
    }

    //PackagingSourceItem sourceItem = ((SourceItemNode)node).getSourceItem();
    //if (sourceItem == null) return null;
    //
    //final StructureConfigurableContext context = getContext();
    //if (sourceItem instanceof ModuleOutputSourceItem) {
    //  return new ModuleProjectStructureElement(context, ((ModuleOutputSourceItem)sourceItem).getModule());
    //}
    //else if (sourceItem instanceof LibrarySourceItem) {
    //  return new LibraryProjectStructureElement(((LibrarySourceItem)sourceItem).getLibrary());
    //}
    //else if (sourceItem instanceof ArtifactSourceItem) {
    //  return myArtifactContext.getOrCreateArtifactElement(((ArtifactSourceItem)sourceItem).getArtifact());
    //}
    return null;
  }
}
