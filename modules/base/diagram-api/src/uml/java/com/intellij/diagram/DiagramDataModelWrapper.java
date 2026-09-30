/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.intellij.diagram;

import com.intellij.openapi.graph.builder.GraphDataModel;
import com.intellij.openapi.util.Disposer;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;


/**
 * @author Konstantin Bulenkov
 */
public class DiagramDataModelWrapper extends GraphDataModel<DiagramNode, DiagramEdge> {
  private final DiagramDataModel myModel;

  public DiagramDataModelWrapper(DiagramDataModel model) {
    myModel = model;
    Disposer.register(this, model);
  }

  public DiagramDataModel getModel() {
    return myModel;
  }  

  @NotNull
  @Override
  public Collection<DiagramNode> getNodes() {
    return myModel.getNodes();
  }

  @NotNull
  @Override
  public Collection<DiagramEdge> getEdges() {
    return myModel.getEdges();
  }

  @NotNull
  @Override
  public DiagramNode getSourceNode(DiagramEdge diagramEdge) {
    return myModel.getSourceNode(diagramEdge);
  }

  @NotNull
  @Override
  public DiagramNode getTargetNode(DiagramEdge diagramEdge) {
    return myModel.getTargetNode(diagramEdge);
  }

  @NotNull
  @Override
  public String getNodeName(DiagramNode diagramNode) {
    return myModel.getNodeName(diagramNode);
  }

  @NotNull
  @Override
  public String getEdgeName(DiagramEdge diagramEdge) {
    return myModel.getEdgeName(diagramEdge);
  }

  @Override
  public DiagramEdge createEdge(@NotNull DiagramNode from, @NotNull DiagramNode to) {
    return myModel.createEdge(from, to);
  }

  public void dispose() {
    myModel.dispose();
  }

  
}
