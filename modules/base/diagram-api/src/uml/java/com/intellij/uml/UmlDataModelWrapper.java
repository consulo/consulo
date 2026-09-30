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

package com.intellij.uml;

import com.intellij.openapi.graph.builder.GraphDataModel;
import com.intellij.openapi.util.Disposer;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;


/**
 * @author Konstantin Bulenkov
 */
public class UmlDataModelWrapper extends GraphDataModel<UmlNode, UmlEdge> {
  private final UmlDataModel myModel;

  public UmlDataModelWrapper(UmlDataModel model) {
    myModel = model;
    Disposer.register(this, model);
  }

  public UmlDataModel getModel() {
    return myModel;
  }  

  @NotNull
  @Override
  public Collection<UmlNode> getNodes() {
    return myModel.getNodes();
  }

  @NotNull
  @Override
  public Collection<UmlEdge> getEdges() {
    return myModel.getEdges();
  }

  @NotNull
  @Override
  public UmlNode getSourceNode(UmlEdge umlEdge) {
    return myModel.getSourceNode(umlEdge);
  }

  @NotNull
  @Override
  public UmlNode getTargetNode(UmlEdge umlEdge) {
    return myModel.getTargetNode(umlEdge);
  }

  @NotNull
  @Override
  public String getNodeName(UmlNode umlNode) {
    return myModel.getNodeName(umlNode);
  }

  @NotNull
  @Override
  public String getEdgeName(UmlEdge umlEdge) {
    return myModel.getEdgeName(umlEdge);
  }

  @Override
  public UmlEdge createEdge(@NotNull UmlNode from, @NotNull UmlNode to) {
    return myModel.createEdge(from, to);
  }

  public void dispose() {
    myModel.dispose();
  }

  
}
