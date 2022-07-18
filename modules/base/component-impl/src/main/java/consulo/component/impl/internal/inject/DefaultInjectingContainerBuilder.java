/*
 * Copyright 2013-2018 consulo.io
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
package consulo.component.impl.internal.inject;

import consulo.component.internal.inject.InjectingContainer;
import consulo.component.internal.inject.InjectingContainerBuilder;
import consulo.component.internal.inject.InjectingKey;
import consulo.component.internal.inject.InjectingPoint;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

/**
 * @author VISTALL
 * @since 2018-08-23
 */
class DefaultInjectingContainerBuilder implements InjectingContainerBuilder {
  private DefaultInjectingContainer myParent;
  private Map<InjectingKey, DefaultInjectingPoint> myPoints = new HashMap<>();

  public DefaultInjectingContainerBuilder(DefaultInjectingContainer parent) {
    myParent = parent;
  }

  @Nonnull
  @Override
  @SuppressWarnings("unchecked")
  public <T> InjectingPoint<T> bind(@Nonnull InjectingKey<T> key) {
    // null points mean free builder
    if (myPoints == null) {
      throw new IllegalArgumentException("Already build container");
    }

    DefaultInjectingPoint<T> point = myPoints.get(key);
    if (point != null) {
      throw new IllegalArgumentException("Rebind " + key);
    }

    point = new DefaultInjectingPoint<>(key);
    myPoints.put(key, point);
    return point;
  }

  @Nonnull
  @Override
  public InjectingContainer build() {
    Map<InjectingKey, DefaultInjectingPoint> points = myPoints;
    DefaultInjectingContainer parent = myParent;

    // free resources
    myPoints = null;
    myParent = null;

    DefaultInjectingContainer container = new DefaultInjectingContainer(parent, points.size());
    for (Map.Entry<InjectingKey, DefaultInjectingPoint> entry : points.entrySet()) {
      container.add(entry.getKey(), entry.getValue());
    }
    return container;
  }
}
