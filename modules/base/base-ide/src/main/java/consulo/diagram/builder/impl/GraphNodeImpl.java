/*
 * Copyright 2013 must-be.org
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
package consulo.diagram.builder.impl;

import consulo.diagram.builder.GraphNode;
import consulo.diagram.builder.GraphPositionStrategy;
import consulo.ui.image.Image;
import consulo.util.pointers.Named;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 22:44/15.10.13
 */
public class GraphNodeImpl<E> implements GraphNode<E>, Named {
  private final List<GraphNode<?>> myArrowNodes = new ArrayList<>();
  private String myName;
  @Nullable
  private Image myIcon;
  private final E myValue;
  private final GraphPositionStrategy myStrategy;

  public GraphNodeImpl(@Nonnull String name, @Nullable Image icon, @Nullable E value, GraphPositionStrategy strategy) {
    myName = name;
    myIcon = icon;
    myValue = value;
    myStrategy = strategy;
  }

  @Override
  public void makeArrow(@Nonnull GraphNode<?> target) {
    myArrowNodes.add(target);
  }

  @Override
  public E getValue() {
    return myValue;
  }

  @Override
  public GraphPositionStrategy getStrategy() {
    return myStrategy;
  }

  @Override
  @Nonnull
  public List<GraphNode<?>> getArrowNodes() {
    return myArrowNodes;
  }

  @Nonnull
  @Override
  public String getName() {
    return myName;
  }

  @Nullable
  @Override
  public Image getIcon() {
    return myIcon;
  }
}