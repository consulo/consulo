/*
 * Copyright 2013-2022 consulo.io
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
package consulo.application.ui.impl.internal;

import consulo.annotation.component.ComponentProfiles;
import consulo.annotation.component.ServiceImpl;
import consulo.application.ui.ApplicationWindowStateService;
import consulo.application.ui.DimensionService;
import consulo.application.ui.WindowStateService;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.RoamingType;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.util.SimpleModificationTracker;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.project.ui.ProjectWindowStateService;
import consulo.ui.Coordinate2D;
import consulo.ui.Size;
import consulo.util.collection.primitive.objects.ObjectIntMap;
import consulo.util.collection.primitive.objects.ObjectMaps;
import consulo.util.lang.Pair;
import jakarta.inject.Singleton;
import org.jdom.Element;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author VISTALL
 * @since 30/12/2022
 */
@Singleton
@ServiceImpl(profiles = ComponentProfiles.UNIFIED)
@State(name = "DimensionService", storages = @Storage(value = "window.state.xml", roamingType = RoamingType.DISABLED))
public class UnifiedDimensionServiceImpl extends SimpleModificationTracker implements DimensionService, PersistentStateComponent<Element> {
  private final Map<String, Coordinate2D> myKey2Location = new LinkedHashMap<>();
  private final Map<String, Size> myKey2Size = new LinkedHashMap<>();
  private final ObjectIntMap<String> myKey2ExtendedState = ObjectMaps.newObjectIntHashMap();
  private static final String EXTENDED_STATE = "extendedState";
  private static final String KEY = "key";
  private static final String STATE = "state";
  private static final String ELEMENT_LOCATION = "location";
  private static final String ELEMENT_SIZE = "size";
  private static final String ATTRIBUTE_X = "x";
  private static final String ATTRIBUTE_Y = "y";
  private static final String ATTRIBUTE_WIDTH = "width";
  private static final String ATTRIBUTE_HEIGHT = "height";

  @Nonnull
  protected static WindowStateService getWindowStateService(@Nullable Project project) {
    return project == null ? ApplicationWindowStateService.getInstance() : ProjectWindowStateService.getInstance(project);
  }

  @Override
  @Nullable
  public synchronized Coordinate2D getLocation(@Nonnull String key, Project project) {
    Coordinate2D point = project == null ? null : ProjectWindowStateService.getInstance(project).getLocation(key);
    if (point != null) return point;

    Pair<String, Float> pair = resolveScale(key, project);
    point = myKey2Location.get(pair.first);
    if (point != null) {
      float scale = pair.second;
      point = new Coordinate2D(((int)(point.getX() / scale)), ((int)(point.getY() / scale)));
    }
    if (point != null && isOutVisibleScreenArea(point)) {
      point = null;
    }
    return point;
  }

  protected boolean isOutVisibleScreenArea(@Nonnull Coordinate2D coordinate2D) {
    return false;
  }

  @Override
  public synchronized void setLocation(@Nonnull String key, Coordinate2D point, Project project) {
    getWindowStateService(project).putLocation(key, point);
    Pair<String, Float> pair = resolveScale(key, project);
    if (point != null) {
      float scale = pair.second;
      point = new Coordinate2D(((int)(point.getX() * scale)), ((int)(point.getY() * scale)));
      myKey2Location.put(pair.first, point);
    }
    else {
      myKey2Location.remove(key);
    }
    incModificationCount();
  }

  @Override
  @Nullable
  public synchronized Size getSize(@Nonnull String key, Project project) {
    Size size = project == null ? null : ProjectWindowStateService.getInstance(project).getSize(key);
    if (size != null) return size;

    Pair<String, Float> pair = resolveScale(key, project);
    size = myKey2Size.get(pair.first);
    if (size != null) {
      float scale = pair.second;
      size = new Size(((int)(size.getWidth() / scale)), ((int)(size.getHeight() / scale)));
    }
    return size;
  }

  @Override
  public synchronized void setSize(@Nonnull String key, Size size, Project project) {
    getWindowStateService(project).putSize(key, size);
    Pair<String, Float> pair = resolveScale(key, project);
    if (size != null) {
      float scale = pair.second;
      size = new Size((int)(size.getWidth() * scale), (int)(size.getHeight() * scale));
      myKey2Size.put(pair.first, size);
    }
    else {
      myKey2Size.remove(pair.first);
    }
    incModificationCount();
  }

  /**
   * @param key a String key to perform a query for.
   * @return point stored under the specified {@code key}. The method returns
   * {@code null} if there is no stored value under the {@code key}. If point
   * is outside of current screen bounds then the method returns {@code null}. It
   * properly works in multi-monitor configuration.
   * @throws IllegalArgumentException if {@code key} is {@code null}.
   */
  @Override
  @Nullable
  public synchronized Coordinate2D getLocation(String key) {
    return getLocation(key, guessProject());
  }

  /**
   * @return Pair(key, scale) where:
   * key is the HiDPI-aware key,
   * scale is the HiDPI-aware factor to transform size metrics.
   */
  @Nonnull
  protected Pair<String, Float> resolveScale(String key, @Nullable Project project) {
    return Pair.create(key, 1f);
  }

  /**
   * @param key a String key to perform a query for.
   * @return point stored under the specified {@code key}. The method returns
   * {@code null} if there is no stored value under the {@code key}.
   * @throws IllegalArgumentException if {@code key} is {@code null}.
   */
  @Override
  @Nullable
  public synchronized Size getSize(@Nonnull String key) {
    return getSize(key, guessProject());
  }

  /**
   * Store specified {@code point} under the {@code key}. If {@code point} is
   * {@code null} then the value stored under {@code key} will be removed.
   *
   * @param key   a String key to store location for.
   * @param point location to save.
   * @throws IllegalArgumentException if {@code key} is {@code null}.
   */
  @Override
  public synchronized void setLocation(String key, Coordinate2D point) {
    setLocation(key, point, guessProject());
  }

  /**
   * Store specified {@code size} under the {@code key}. If {@code size} is
   * {@code null} then the value stored under {@code key} will be removed.
   *
   * @param key  a String key to to save size for.
   * @param size a Size to save.
   * @throws IllegalArgumentException if {@code key} is {@code null}.
   */
  @Override
  public synchronized void setSize(@Nonnull String key, Size size) {
    setSize(key, size, guessProject());
  }

  @Override
  public Element getState() {
    Element element = new Element("state");
    // Save locations
    for (Map.Entry<String, Coordinate2D> entry : myKey2Location.entrySet()) {
      String key = entry.getKey();
      Coordinate2D point = entry.getValue();

      Element e = new Element(ELEMENT_LOCATION);
      e.setAttribute(KEY, key);
      e.setAttribute(ATTRIBUTE_X, String.valueOf(point.getX()));
      e.setAttribute(ATTRIBUTE_Y, String.valueOf(point.getY()));
      element.addContent(e);
    }

    // Save sizes
    for (Map.Entry<String, Size> entry : myKey2Size.entrySet()) {
      String key = entry.getKey();
      Size size = entry.getValue();

      Element e = new Element(ELEMENT_SIZE);
      e.setAttribute(KEY, key);
      e.setAttribute(ATTRIBUTE_WIDTH, String.valueOf(size.getWidth()));
      e.setAttribute(ATTRIBUTE_HEIGHT, String.valueOf(size.getHeight()));
      element.addContent(e);
    }

    // Save extended states
    myKey2ExtendedState.forEach((key, stateValue) -> {
      Element e = new Element(EXTENDED_STATE);
      e.setAttribute(KEY, key);
      e.setAttribute(STATE, String.valueOf(stateValue));
      element.addContent(e);
    });
    return element;
  }

  @Override
  public void loadState(@Nonnull final Element element) {
    myKey2Location.clear();
    myKey2Size.clear();
    myKey2ExtendedState.clear();

    for (Element e : element.getChildren()) {
      if (ELEMENT_LOCATION.equals(e.getName())) {
        try {
          myKey2Location.put(e.getAttributeValue(KEY), new Coordinate2D(Integer.parseInt(e.getAttributeValue(ATTRIBUTE_X)), Integer.parseInt(e.getAttributeValue(ATTRIBUTE_Y))));
        }
        catch (NumberFormatException ignored) {
        }
      }
      else if (ELEMENT_SIZE.equals(e.getName())) {
        try {
          myKey2Size.put(e.getAttributeValue(KEY), new Size(Integer.parseInt(e.getAttributeValue(ATTRIBUTE_WIDTH)), Integer.parseInt(e.getAttributeValue(ATTRIBUTE_HEIGHT))));
        }
        catch (NumberFormatException ignored) {
        }
      }
      else if (EXTENDED_STATE.equals(e.getName())) {
        try {
          myKey2ExtendedState.putInt(e.getAttributeValue(KEY), Integer.parseInt(e.getAttributeValue(STATE)));
        }
        catch (NumberFormatException ignored) {
        }
      }
    }
  }

  @Nullable
  protected static Project guessProject() {
    final Project[] openProjects = ProjectManager.getInstance().getOpenProjects();
    return openProjects.length == 1 ? openProjects[0] : null;
  }
}
