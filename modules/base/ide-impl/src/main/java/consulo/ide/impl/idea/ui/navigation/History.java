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

package consulo.ide.impl.idea.ui.navigation;

import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.util.concurrent.AsyncResult;
import consulo.util.dataholder.Key;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArraySet;

public final class History {
  public static Key<History> KEY = Key.create("History");

  private final List<Place> myHistory = new ArrayList<>();
  private int myCurrentPos;
  private final Place.Navigator myRoot;

  private boolean myNavigatedNow;
  private final CopyOnWriteArraySet<HistoryListener> myListeners = new CopyOnWriteArraySet<>();

  public History(@Nonnull Place.Navigator root) {
    myRoot = root;
  }

  public void pushQueryPlace() {
    if (isNavigatingNow()) return;

    Place place = query();
    if (place != null) {
      pushPlace(query());
    }
  }

  public void pushPlace(@Nonnull Place place) {
    while (myCurrentPos > 0 && myHistory.size() > 0 && myCurrentPos < myHistory.size() - 1) {
      myHistory.remove(myHistory.size() - 1);
    }

    if (myHistory.size() > 0) {
      Place prev = myHistory.get(myHistory.size() - 1);
      if (prev.equals(place)) return;

      if (prev.isMoreGeneralFor(place)) {
        myHistory.remove(prev);
      }
    }

    addPlace(place);
  }

  private void addPlace(Place place) {
    myHistory.add(place);
    myCurrentPos = myHistory.size() - 1;
  }

  public void pushPlaceForElement(String name, Object value) {
    if (!canNavigateFor(name)) return;

    Place checkPlace = getCheckPlace(name);
    if (checkPlace == null) return;
    pushPlace(checkPlace.cloneForElement(name, value));
  }

  public Place getPlaceForElement(String name, String value) {
    Place checkPlace = getCheckPlace(name);
    if (checkPlace == null) return new Place();

    return checkPlace.cloneForElement(name, value);
  }

  public void navigateTo(Place place) {
    myRoot.navigateTo(place, false);
  }

  public void back() {
    assert canGoBack();
    goThere(myCurrentPos - 1);
  }

  private void goThere(final int nextPos) {
    myNavigatedNow = true;
    final Place next = myHistory.get(nextPos);
    final Place from = getCurrent();
    fireStarted(from, next);
    try {
      AsyncResult<Void> callback = myRoot.navigateTo(next, false);
      callback.doWhenDone(new Runnable() {
        @Override
        public void run() {
          myCurrentPos = nextPos;
        }
      }).doWhenProcessed(new Runnable() {
        @Override
        public void run() {
          myNavigatedNow = false;
          fireFinished(from, next);
        }
      });
    }
    catch (Throwable e) {
      myNavigatedNow = false;
      throw new RuntimeException(e);
    }
  }

  public boolean isNavigatingNow() {
    return myNavigatedNow;
  }

  public boolean canGoBack() {
    return myHistory.size() > 1 && myCurrentPos > 0;
  }

  public void forward() {
    assert canGoForward();
    goThere(myCurrentPos + 1);
  }

  public boolean canGoForward() {
    return myHistory.size() > 1 && myCurrentPos < myHistory.size() - 1;
  }

  public void clear() {
    myHistory.clear();
    myCurrentPos = -1;
  }

  public Place query() {
    Place result = new Place();
    myRoot.queryPlace(result);
    return result;
  }

  private Place getCurrent() {
    if (myCurrentPos >= 0 && myCurrentPos < myHistory.size()) {
      return myHistory.get(myCurrentPos);
    } else {
      return null;
    }
  }

  private boolean canNavigateFor(String pathElement) {
    if (isNavigatingNow()) return false;

    Place checkPlace = getCheckPlace(pathElement);

    return checkPlace != null && checkPlace.getPath(pathElement) != null;
  }

  @Nullable
  private Place getCheckPlace(String pathElement) {
    Place checkPlace = getCurrent();
    if (checkPlace == null || checkPlace.getPath(pathElement) == null) {
      checkPlace = query();
    }

    return checkPlace != null && checkPlace.getPath(pathElement) != null ? checkPlace : null;
  }

  public void addListener(final HistoryListener listener, Disposable parent) {
    myListeners.add(listener);
    Disposer.register(parent, new Disposable() {
      @Override
      public void dispose() {
        myListeners.remove(listener);
      }
    });
  }

  private void fireStarted(Place from, Place to) {
    for (HistoryListener each : myListeners) {
      each.navigationStarted(from, to);
    }
  }

  private void fireFinished(Place from, Place to) {
    for (HistoryListener each : myListeners) {
      each.navigationFinished(from, to);
    }
  }


}
