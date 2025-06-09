/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.application.ui;

import consulo.project.Project;
import consulo.ui.Point2D;
import consulo.ui.Rectangle2D;
import consulo.ui.Size2D;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.*;

/**
 * @author Sergey.Malenkov
 */
public interface WindowStateService {

  /**
   * Returns a window state by the specified key.
   * Also it adds a listener to save a modified state automatically.
   *
   * @param key    an unique string key
   * @param window a window state which should be watched for
   * @return a corresponding window state
   */
  default WindowState getState(@Nonnull String key, @Nonnull Window window) {
    return getStateFor(getProject(), key, window);
  }

  /**
   * Returns a window state by the given project and the specified key.
   * Also it adds a listener to save a modified state automatically.
   *
   * @param project an project that specifies a main screen
   * @param key     an unique string key
   * @param window  a window state which should be watched for
   * @return a corresponding window state
   */
  default WindowState getStateFor(@Nullable Project project, @Nonnull String key, @Nonnull Window window) {
    throw new AbstractMethodError("desktop only");
  }

  /**
   * Returns a location that corresponds to the specified key or {@code null}
   * if a location does not exist or it is outside of visible area.
   *
   * @param key an unique string key
   * @return a corresponding location
   */
  default Point2D getLocation(@Nonnull String key) {
    return getLocationFor(getProject(), key);
  }

  /**
   * Returns a location that corresponds to the given screen and the specified key or {@code null}
   * if a location does not exist or it is outside of visible area.
   * A screen can be specified by {@link Project}, {@link Window}, or {@link GraphicsDevice}.
   *
   * @param object an object that specifies a screen to which a location belongs
   * @param key    an unique string key
   * @return a corresponding location
   */
  public abstract Point2D getLocationFor(Object object, @Nonnull String key);

  /**
   * Stores the specified location that corresponds to the specified key.
   * If it is {@code null} the stored location will be removed.
   *
   * @param key an unique string key
   */
  default void putLocation(@Nonnull String key, Point2D location) {
    putLocationFor(getProject(), key, location);
  }

  /**
   * Stores the specified location that corresponds to the given screen and the specified key.
   * If it is {@code null} the stored location will be removed.
   * A screen can be specified by {@link Project}, {@link Window}, or {@link GraphicsDevice}.
   * Do not use a screen which is calculated from the specified location.
   *
   * @param object an object that specifies a screen to which a location belongs
   * @param key    an unique string key
   */
  public abstract void putLocationFor(Object object, @Nonnull String key, Point2D location);

  /**
   * Returns a size that corresponds to the specified key or {@code null}
   * if a size does not exist.
   *
   * @param key an unique string key
   * @return a corresponding size
   */
  default Size2D getSize(@Nonnull String key) {
    return getSizeFor(getProject(), key);
  }

  /**
   * Returns a size that corresponds to the given screen and the specified key or {@code null}
   * if a size does not exist.
   * A screen can be specified by {@link Project}, {@link Window}, or {@link GraphicsDevice}.
   *
   * @param object an object that specifies a screen to which a size belongs
   * @param key    an unique string key
   * @return a corresponding size
   */
  public abstract Size2D getSizeFor(Object object, @Nonnull String key);

  /**
   * Stores the specified size that corresponds to the specified key.
   * If it is {@code null} the stored size will be removed.
   *
   * @param key an unique string key
   */
  default void putSize(@Nonnull String key, Size2D size) {
    putSizeFor(getProject(), key, size);
  }

  /**
   * Stores the specified size that corresponds to the given screen and the specified key.
   * If it is {@code null} the stored size will be removed.
   * A screen can be specified by {@link Project}, {@link Window}, or {@link GraphicsDevice}.
   * Do not use a screen which is calculated from the specified size.
   *
   * @param object an object that specifies a screen to which a size belongs
   * @param key    an unique string key
   */
  public abstract void putSizeFor(Object object, @Nonnull String key, Size2D size);

  /**
   * Returns a bounds that corresponds to the specified key or {@code null}
   * if a bounds does not exist or it is outside of visible area.
   *
   * @param key an unique string key
   * @return a corresponding bounds
   */
  default Rectangle2D getBounds(@Nonnull String key) {
    return getBoundsFor(getProject(), key);
  }

  /**
   * Returns a bounds that corresponds to the given screen and the specified key or {@code null}
   * if a bounds does not exist or it is outside of visible area.
   * A screen can be specified by {@link Project}, {@link Window}, or {@link GraphicsDevice}.
   *
   * @param object an object that specifies a screen to which a bounds belongs
   * @param key    an unique string key
   * @return a corresponding bounds
   */
  public abstract Rectangle2D getBoundsFor(Object object, @Nonnull String key);

  /**
   * Stores the specified bounds that corresponds to the specified key.
   * If it is {@code null} the stored bounds will be removed.
   *
   * @param key an unique string key
   */
  default void putBounds(@Nonnull String key, Rectangle2D bounds) {
    putBoundsFor(getProject(), key, bounds);
  }

  /**
   * Stores the specified bounds that corresponds to the given screen and the specified key.
   * If it is {@code null} the stored bounds will be removed.
   * A screen can be specified by {@link Project}, {@link Window}, or {@link GraphicsDevice}.
   * Do not use a screen which is calculated from the specified bounds.
   *
   * @param object an object that specifies a screen to which a bounds belongs
   * @param key    an unique string key
   */
  public abstract void putBoundsFor(Object object, @Nonnull String key, Rectangle2D bounds);

  @Nullable
  default Project getProject() {
    return null;
  }
}
