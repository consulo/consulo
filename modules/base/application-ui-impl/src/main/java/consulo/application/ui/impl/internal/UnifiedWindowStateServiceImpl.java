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

import consulo.application.ui.UISettings;
import consulo.application.ui.WindowState;
import consulo.application.ui.WindowStateService;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.util.ModificationTracker;
import consulo.project.Project;
import consulo.ui.Coordinate2D;
import consulo.ui.Rectangle2D;
import consulo.ui.Size;
import consulo.ui.ex.util.UIXmlSerializeUtil;
import consulo.util.lang.StringUtil;
import org.jdom.Element;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 30/12/2022
 */
public abstract class UnifiedWindowStateServiceImpl<GC> implements WindowStateService, ModificationTracker, PersistentStateComponent<Element> {
  protected final class CachedState<G extends GC> {
    private Rectangle2D myScreen;
    private Coordinate2D myLocation;
    private Size mySize;
    private boolean myMaximized;
    private boolean myFullScreen;
    private long myTimeStamp;

    protected CachedState() {
    }

    @SuppressWarnings("unchecked")
    <T> T get(@Nonnull Class<T> type, @Nullable Rectangle2D screen) {
      Coordinate2D location = apply(Coordinate2D::new, myLocation);
      Size size = apply(Size::new, mySize);
      // convert location and size according to the given screen
      if (myScreen != null && screen != null && !screen.isEmpty()) {
        double w = myScreen.getWidth() / screen.getWidth();
        double h = myScreen.getHeight() / screen.getHeight();
        if (location != null) location = new Coordinate2D(((int)(screen.getX() + (location.getX() - myScreen.getX()) / w)), ((int)(screen.getY() + (location.getY() - myScreen.getY()) / h)));
        if (size != null) size = new Size(((int)(size.getWidth() / w)), ((int)(size.getHeight() / h)));
        if (!isVisible(location, size)) return null; // adjusted state is not visible
      }
      if (type == Coordinate2D.class) return (T)location;
      if (type == Size.class) return (T)size;
      if (type == Rectangle2D.class) return location == null || size == null ? null : (T)new Rectangle2D(location, size);
      if (type != WindowState.class) throw new IllegalArgumentException();
      // copy a current state
      BaseWindowStateBean state = newWindowStateBean();
      state.setLocation(location);
      state.setSize(size);
      state.setMaximized(myMaximized);
      state.setFullScreen(myFullScreen);
      return (T)state;
    }

    private boolean set(Coordinate2D location, boolean locationSet, Size size, boolean sizeSet, boolean maximized, boolean maximizedSet, boolean fullScreen, boolean fullScreenSet) {
      if (locationSet) {
        myLocation = apply(Coordinate2D::new, location);
      }
      if (sizeSet) {
        mySize = apply(Size::new, size);
      }
      if (maximizedSet) {
        myMaximized = maximized;
      }
      if (fullScreenSet) {
        myFullScreen = fullScreen;
      }
      if (myLocation == null && mySize == null) return false;
      // update timestamp of modified state
      myTimeStamp = System.currentTimeMillis();
      return true;
    }

    void updateScreenRectangle(@Nullable G configuration) {
      myScreen = myLocation == null
                 ? getScreenRectangle(configuration)
                 : mySize == null ? getScreenRectangle(myLocation) : getScreenRectangle(new Coordinate2D(myLocation.getX() + mySize.getWidth() / 2, myLocation.getY() + mySize.getHeight() / 2));
    }
  }

  private static final String KEY = "key";
  private static final String STATE = "state";
  private static final String MAXIMIZED = "maximized";
  private static final String FULL_SCREEN = "full-screen";
  private static final String TIMESTAMP = "timestamp";
  private static final String SCREEN = "screen";

  protected final AtomicLong myModificationCount = new AtomicLong();
  protected final Map<String, Runnable> myRunnableMap = new TreeMap<>();
  protected final Map<String, CachedState<GC>> myStateMap = new TreeMap<>();

  private Project myProject;

  protected UnifiedWindowStateServiceImpl(@Nullable Project project) {
    myProject = project;
  }


  protected <T> T getFor(Object object, @Nonnull String key, @Nonnull Class<T> type) {
    if (isHeadless()) return null;
    if (UISettings.getInstance().getPresentationMode()) key += ".inPresentationMode"; // separate key for the presentation mode
    GC configuration = getConfiguration(object);
    synchronized (myStateMap) {
      CachedState<GC> state = myStateMap.get(getAbsoluteKey(configuration, key));
      if (isVisible(state)) return state.get(type, null);

      state = myStateMap.get(key);
      return state == null ? null : state.get(type, state.myScreen == null ? null : getScreenRectangle(configuration));
    }
  }

  protected void putFor(Object object,
                        @Nonnull String key,
                        Coordinate2D location,
                        boolean locationSet,
                        Size size,
                        boolean sizeSet,
                        boolean maximized,
                        boolean maximizedSet,
                        boolean fullScreen,
                        boolean fullScreenSet) {
    if (isHeadless()) return;
    if (UISettings.getInstance().getPresentationMode()) key += ".inPresentationMode"; // separate key for the presentation mode
    GC configuration = getConfiguration(object);
    synchronized (myStateMap) {
      put(getAbsoluteKey(configuration, key), location, locationSet, size, sizeSet, maximized, maximizedSet, fullScreen, fullScreenSet);

      CachedState<GC> state = put(key, location, locationSet, size, sizeSet, maximized, maximizedSet, fullScreen, fullScreenSet);
      if (state != null) state.updateScreenRectangle(configuration); // update a screen to adjust stored state
    }
    myModificationCount.getAndIncrement();
  }

  public boolean isVisible(CachedState state) {
    return state != null && isVisible(state.myLocation, state.mySize);
  }

  @Nonnull
  protected Rectangle2D getScreenRectangle(@Nullable GC configuration) {
    // TODO this need to be reworked
    return new Rectangle2D(0, 0);
  }

  protected Rectangle2D getScreenRectangle(@Nonnull Coordinate2D location) {
    // TODO this need to be reworked
    return new Rectangle2D(location.getX(), location.getY(), 0, 0);
  }

  public boolean isVisible(Coordinate2D location, Size size) {
    return true;
  }

  @Nonnull
  protected String getAbsoluteKey(@Nullable GC configuration, @Nonnull String key) {
    return key;
  }

  @Nullable
  protected abstract GC getConfiguration(Object key);

  protected boolean isHeadless() {
    return false;
  }

  @Override
  public Coordinate2D getLocationFor(Object object, @Nonnull String key) {
    return getFor(object, key, Coordinate2D.class);
  }

  @Override
  public void putLocationFor(Object object, @Nonnull String key, Coordinate2D location) {
    putFor(object, key, location, true, null, false, false, false, false, false);
  }

  @Override
  public Size getSizeFor(Object object, @Nonnull String key) {
    return getFor(object, key, Size.class);
  }

  @Override
  public void putSizeFor(Object object, @Nonnull String key, Size size) {
    putFor(object, key, null, false, size, true, false, false, false, false);
  }

  @Override
  public Rectangle2D getBoundsFor(Object object, @Nonnull String key) {
    return getFor(object, key, Rectangle2D.class);
  }

  @Override
  public void putBoundsFor(Object object, @Nonnull String key, Rectangle2D bounds) {
    Coordinate2D location = apply(Rectangle2D::getCoordinate, bounds);
    Size size = apply(Rectangle2D::getSize, bounds);
    putFor(object, key, location, true, size, true, false, false, false, false);
  }

  @Override
  public final void loadState(@Nonnull Element element) {
    synchronized (myStateMap) {
      myStateMap.clear();
      for (Element child : element.getChildren()) {
        if (!STATE.equals(child.getName())) continue; // ignore unexpected element

        long current = System.currentTimeMillis();
        long timestamp = StringUtil.parseLong(child.getAttributeValue(TIMESTAMP), current);
        if (TimeUnit.DAYS.toMillis(100) <= (current - timestamp)) continue; // ignore old elements

        String key = child.getAttributeValue(KEY);
        if (StringUtil.isEmpty(key)) continue; // unexpected key

        Coordinate2D location = UIXmlSerializeUtil.getLocation(child);
        Size size = UIXmlSerializeUtil.getSize(child);
        if (location == null && size == null) continue; // unexpected value

        CachedState<GC> state = newCachedState();
        state.myLocation = location;
        state.mySize = size;
        state.myMaximized = Boolean.parseBoolean(child.getAttributeValue(MAXIMIZED));
        state.myFullScreen = Boolean.parseBoolean(child.getAttributeValue(FULL_SCREEN));
        state.myScreen = apply(UIXmlSerializeUtil::getBounds, child.getChild(SCREEN));
        state.myTimeStamp = timestamp;
        myStateMap.put(key, state);
      }
    }
  }

  @Override
  public final Element getState() {
    Element element = new Element(STATE);
    synchronized (myStateMap) {
      for (Map.Entry<String, CachedState<GC>> entry : myStateMap.entrySet()) {
        String key = entry.getKey();
        if (key != null) {
          CachedState<GC> state = entry.getValue();
          Element child = new Element(STATE);
          if (state.myLocation != null) {
            UIXmlSerializeUtil.setLocation(child, state.myLocation);
          }
          if (state.mySize != null) {
            UIXmlSerializeUtil.setSize(child, state.mySize);
          }
          if (state.myMaximized) {
            child.setAttribute(MAXIMIZED, Boolean.toString(true));
          }
          if (state.myFullScreen) {
            child.setAttribute(FULL_SCREEN, Boolean.toString(true));
          }
          if (state.myScreen != null) {
            child.addContent(UIXmlSerializeUtil.setBounds(new Element(SCREEN), state.myScreen));
          }
          child.setAttribute(KEY, key);
          child.setAttribute(TIMESTAMP, Long.toString(state.myTimeStamp));
          element.addContent(child);
        }
      }
    }
    return element;
  }

  @Nullable
  private CachedState<GC> put(@Nonnull String key,
                              @Nullable Coordinate2D location,
                              boolean locationSet,
                              @Nullable Size size,
                              boolean sizeSet,
                              boolean maximized,
                              boolean maximizedSet,
                              boolean fullScreen,
                              boolean fullScreenSet) {
    CachedState<GC> state = myStateMap.get(key);
    if (state == null) {
      state = newCachedState();
      if (!state.set(location, locationSet, size, sizeSet, maximized, maximizedSet, fullScreen, fullScreenSet)) return null;
      myStateMap.put(key, state);
      return state;
    }
    else {
      if (state.set(location, locationSet, size, sizeSet, maximized, maximizedSet, fullScreen, fullScreenSet)) return state;
      myStateMap.remove(key);
      return null;
    }
  }

  protected CachedState<GC> newCachedState() {
    return new CachedState<>();
  }

  protected BaseWindowStateBean newWindowStateBean() {
    return new UnifiedWindowStateBean();
  }

  @Override
  public long getModificationCount() {
    synchronized (myRunnableMap) {
      myRunnableMap.values().forEach(Runnable::run);
    }
    return myModificationCount.get();
  }

  @Nullable
  @Override
  public Project getProject() {
    return myProject;
  }

  @Nullable
  protected static <T, R> R apply(@Nonnull Function<T, R> function, @Nullable T value) {
    return value == null ? null : function.apply(value);
  }
}
