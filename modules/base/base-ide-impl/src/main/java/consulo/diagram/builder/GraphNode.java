/*
 * Copyright 2013-2016 consulo.io
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
package consulo.diagram.builder;

import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * @author VISTALL
 * @since 22:43/15.10.13
 */
public interface GraphNode<T> {
  /**
   * Create arrow to target
   *  [THIS NODE] -> [TARGET NODE]
   * @param target
   */
  void makeArrow(@Nonnull GraphNode<?> target);

  @Nonnull
  List<GraphNode<?>> getArrowNodes();

  @Nonnull
  String getName();

  @Nullable
  Image getIcon();

  T getValue();

  GraphPositionStrategy getStrategy();
}
