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
package consulo.injecting.pico;

import consulo.injecting.InjectingContainer;
import consulo.injecting.InjectingContainerBuilder;
import consulo.injecting.InjectingPoint;
import consulo.injecting.key.InjectingKey;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

/**
 * @author VISTALL
 * @since 2018-08-23
 */
class PicoInjectingContainerBuilder implements InjectingContainerBuilder {
  private PicoInjectingContainer myParent;
  private Map<InjectingKey, PicoInjectingPoint> myPoints = new HashMap<>();

  public PicoInjectingContainerBuilder(PicoInjectingContainer parent) {
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

    PicoInjectingPoint<T> point = myPoints.get(key);
    if (point != null) {
      throw new IllegalArgumentException("Rebind " + key);
    }

    point = new PicoInjectingPoint<>(key);
    myPoints.put(key, point);
    return point;
  }

  @Nonnull
  @Override
  public InjectingContainer build() {
    Map<InjectingKey, PicoInjectingPoint> points = myPoints;
    PicoInjectingContainer parent = myParent;

    // free resources
    myPoints = null;
    myParent = null;

    PicoInjectingContainer container = new PicoInjectingContainer(parent, points.size());
    for (Map.Entry<InjectingKey, PicoInjectingPoint> entry : points.entrySet()) {
      container.add(entry.getKey(), entry.getValue());
    }
    return container;
  }
}
