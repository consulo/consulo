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

package com.intellij.execution.ui.layout.impl;

import com.intellij.execution.ui.layout.LayoutAttractionPolicy;
import com.intellij.execution.ui.layout.PlaceInGrid;
import com.intellij.execution.ui.layout.Tab;
import com.intellij.openapi.util.Comparing;
import consulo.util.dataholder.Key;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.content.Content;
import com.intellij.util.xmlb.SkipDefaultValuesSerializationFilters;
import com.intellij.util.xmlb.XmlSerializer;
import consulo.ui.image.Image;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public class RunnerLayout  {
  public static final Key<Integer> DEFAULT_INDEX = Key.create("RunnerLayoutDefaultIndex");
  public static final Key<Integer> DROP_INDEX = Key.create("RunnerLayoutDropIndex");
  private final String myID;

  protected Map<String, ViewImpl> myViews = new LinkedHashMap<>();
  private final Map<String, ViewImpl.Default> myDefaultViews = new HashMap<>();

  protected Set<TabImpl> myTabs = new TreeSet<>((o1, o2) -> o1.getIndex() - o2.getIndex());
  private final Map<Integer, TabImpl.Default> myDefaultTabs = new HashMap<>();

  protected General myGeneral = new General();
  private final Map<String, Pair<String, LayoutAttractionPolicy>> myDefaultFocus = new HashMap<>();
  private Set<String> myLightWeightIds = null;


  public RunnerLayout(@Nonnull String ID) {
    myID = ID;
  }

  @Nullable
  public String getDefaultDisplayName(final int defaultIndex) {
    final TabImpl.Default tab = myDefaultTabs.get(defaultIndex);
    return tab != null ? tab.myDisplayName : null;
  }

  @Nonnull
  public TabImpl getOrCreateTab(final int index) {
    TabImpl tab = findTab(index);
    if (tab != null) return tab;

    tab = createNewTab(index);

    return tab;
  }

  @Nonnull
  private TabImpl createNewTab(final int index) {
    final TabImpl.Default defaultTab = getOrCreateDefaultTab(index);
    final TabImpl tab = defaultTab.createTab();

    myTabs.add(tab);

    return tab;
  }

  @Nonnull
  private TabImpl.Default getOrCreateDefaultTab(final int index) {
    TabImpl.Default tab = myDefaultTabs.get(index);
    if (tab == null) {
      tab = new TabImpl.Default(index, null, null);
      myDefaultTabs.put(index, tab);
    }
    return tab;
  }

  @Nonnull
  public TabImpl createNewTab() {
    return createNewTab(myTabs.size());
  }

  private boolean isUsed(@Nonnull TabImpl tab) {
    for (ViewImpl each : myViews.values()) {
      if (each.getTab() == tab) return true;
    }

    return false;
  }

  @Nullable
  protected TabImpl findTab(int index) {
    for (TabImpl each : myTabs) {
      if (index == each.getIndex()) return each;
    }

    return null;
  }

  @Nonnull
  public Element getState() {
    return write(new Element("layout"));
  }

  public void loadState(@Nonnull Element state) {
    read(state);
  }

  @Nonnull
  public Element read(@Nonnull Element parentNode) {
    List<Element> tabs = parentNode.getChildren(StringUtil.getShortName(TabImpl.class.getName()));
    for (Element eachTabElement : tabs) {
      TabImpl eachTab = XmlSerializer.deserialize(eachTabElement, TabImpl.class);
      assert eachTab != null;
      XmlSerializer.deserializeInto(getOrCreateTab(eachTab.getIndex()), eachTabElement);
    }

    final List views = parentNode.getChildren(StringUtil.getShortName(ViewImpl.class.getName()));
    for (Object content : views) {
      final ViewImpl state = new ViewImpl(this, (Element)content);
      myViews.put(state.getID(), state);
    }

    XmlSerializer.deserializeInto(myGeneral, parentNode.getChild(StringUtil.getShortName(myGeneral.getClass().getName(), '$')));

    return parentNode;
  }

  @Nonnull
  public Element write(@Nonnull Element parentNode) {
    for (ViewImpl eachState : myViews.values()) {
      if (myLightWeightIds != null && myLightWeightIds.contains(eachState.getID())) {
        continue;
      }
      parentNode.addContent(XmlSerializer.serialize(eachState));
    }

    SkipDefaultValuesSerializationFilters filter = new SkipDefaultValuesSerializationFilters();
    for (TabImpl eachTab : myTabs) {
      if (isUsed(eachTab)) {
        parentNode.addContent(XmlSerializer.serialize(eachTab, filter));
      }
    }

    parentNode.addContent(XmlSerializer.serialize(myGeneral, filter));

    return parentNode;
  }


  public void resetToDefault() {
    myViews.clear();
    myTabs.clear();
  }

  public boolean isToolbarHorizontal() {
    return false;
  }

  public void setToolbarHorizontal(boolean horizontal) {
    myGeneral.horizontalToolbar = horizontal;
  }

  @Nonnull
  public ViewImpl getStateFor(@Nonnull Content content) {
    return getOrCreateView(getOrCreateContentId(content));
  }

  public void clearStateFor(@Nonnull Content content) {
    String id = getOrCreateContentId(content);
    myDefaultViews.remove(id);
    final ViewImpl view = myViews.remove(id);
    if (view != null) {
      final Tab tab = view.getTab();
      if (tab instanceof TabImpl) {
        myTabs.remove(tab);
      }
    }
  }

  @Nonnull
  private static String getOrCreateContentId(@Nonnull Content content) {
    @NonNls String id = content.getUserData(ViewImpl.ID);
    if (id == null) {
      id = "UnknownView-" + content.getDisplayName();
      content.putUserData(ViewImpl.ID, id);
    }
    return id;
  }

  @Nonnull
  private ViewImpl getOrCreateView(@Nonnull String id) {
    ViewImpl view = myViews.get(id);
    if (view == null) {
      view = getOrCreateDefault(id).createView(this);
      myViews.put(id, view);
    }
    return view;
  }

  @Nonnull
  private ViewImpl.Default getOrCreateDefault(@Nonnull String id) {
    if (myDefaultViews.containsKey(id)) {
      return myDefaultViews.get(id);
    }
    return setDefault(id, Integer.MAX_VALUE, PlaceInGrid.bottom, false);
  }


  @Nonnull
  public TabImpl.Default setDefault(int tabID, String displayName, Image icon) {
    final TabImpl.Default tab = new TabImpl.Default(tabID, displayName, icon);
    myDefaultTabs.put(tabID, tab);
    return tab;
  }

  @Nonnull
  public ViewImpl.Default setDefault(@Nonnull String id, int tabIndex, @Nonnull PlaceInGrid placeInGrid, boolean isMinimized) {
    final ViewImpl.Default view = new ViewImpl.Default(id, tabIndex, placeInGrid, isMinimized);
    myDefaultViews.put(id, view);
    return view;
  }

  @Nonnull
  public PlaceInGrid getDefaultGridPlace(@Nonnull Content content) {
    return getOrCreateDefault(getOrCreateContentId(content)).getPlaceInGrid();
  }

  public boolean isToFocus(final String id, @Nonnull String condition) {
    return Comparing.equal(id, getToFocus(condition));
  }

  public void setToFocus(final String id, @Nonnull String condition) {
    myGeneral.focusOnCondition.put(condition, id);
  }

  public void setDefaultToFocus(@Nonnull String id, @Nonnull String condition, @Nonnull final LayoutAttractionPolicy policy) {
    myDefaultFocus.put(condition, Pair.create(id, policy));
  }

  @Nullable
  public String getToFocus(@Nonnull String condition) {
    return myGeneral.focusOnCondition.containsKey(condition) ? myGeneral.focusOnCondition.get(condition) :
           myDefaultFocus.containsKey(condition) ? myDefaultFocus.get(condition).getFirst() : null;
  }

  @Nonnull
  public LayoutAttractionPolicy getAttractionPolicy(@Nonnull String condition) {
    final Pair<String, LayoutAttractionPolicy> pair = myDefaultFocus.get(condition);
    return pair == null ? new LayoutAttractionPolicy.FocusOnce() : pair.getSecond();
  }

  /**
   *   States of contents marked as "lightweight" won't be persisted
   */
  public void setLightWeight(Content content) {
    if (myLightWeightIds == null) {
      myLightWeightIds = new HashSet<>();
    }
    myLightWeightIds.add(getOrCreateContentId(content));
  }

  public static class General {
    public volatile boolean horizontalToolbar = false;
    public volatile Map<String, String> focusOnCondition = new HashMap<>();
  }
}
