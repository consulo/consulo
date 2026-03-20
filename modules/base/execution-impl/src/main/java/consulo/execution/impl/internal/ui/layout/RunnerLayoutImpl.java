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

package consulo.execution.impl.internal.ui.layout;

import consulo.execution.internal.layout.RunnerLayout;
import consulo.execution.internal.layout.Tab;
import consulo.execution.ui.layout.LayoutAttractionPolicy;
import consulo.execution.ui.layout.PlaceInGrid;
import consulo.ui.ex.content.Content;
import consulo.ui.image.Image;
import consulo.util.dataholder.Key;
import consulo.util.lang.Comparing;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import consulo.util.xml.serializer.SkipDefaultValuesSerializationFilters;
import consulo.util.xml.serializer.XmlSerializer;
import org.jspecify.annotations.Nullable;
import org.jdom.Element;

import java.util.*;

public class RunnerLayoutImpl implements RunnerLayout {
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

    public RunnerLayoutImpl(String ID) {
        myID = ID;
    }

    public @Nullable String getDefaultDisplayName(int defaultIndex) {
        TabImpl.Default tab = myDefaultTabs.get(defaultIndex);
        return tab != null ? tab.myDisplayName : null;
    }

    
    public TabImpl getOrCreateTab(int index) {
        TabImpl tab = findTab(index);
        if (tab != null) {
            return tab;
        }

        tab = createNewTab(index);

        return tab;
    }

    
    private TabImpl createNewTab(int index) {
        TabImpl.Default defaultTab = getOrCreateDefaultTab(index);
        TabImpl tab = defaultTab.createTab();

        myTabs.add(tab);

        return tab;
    }

    
    private TabImpl.Default getOrCreateDefaultTab(int index) {
        TabImpl.Default tab = myDefaultTabs.get(index);
        if (tab == null) {
            tab = new TabImpl.Default(index, null, null);
            myDefaultTabs.put(index, tab);
        }
        return tab;
    }

    
    public TabImpl createNewTab() {
        return createNewTab(myTabs.size());
    }

    private boolean isUsed(TabImpl tab) {
        for (ViewImpl each : myViews.values()) {
            if (each.getTab() == tab) {
                return true;
            }
        }

        return false;
    }

    protected @Nullable TabImpl findTab(int index) {
        for (TabImpl each : myTabs) {
            if (index == each.getIndex()) {
                return each;
            }
        }

        return null;
    }

    
    public Element getState() {
        return write(new Element("layout"));
    }

    public void loadState(Element state) {
        read(state);
    }

    
    public Element read(Element parentNode) {
        List<Element> tabs = parentNode.getChildren(StringUtil.getShortName(TabImpl.class.getName()));
        for (Element eachTabElement : tabs) {
            TabImpl eachTab = XmlSerializer.deserialize(eachTabElement, TabImpl.class);
            assert eachTab != null;
            XmlSerializer.deserializeInto(getOrCreateTab(eachTab.getIndex()), eachTabElement);
        }

        List views = parentNode.getChildren(StringUtil.getShortName(ViewImpl.class.getName()));
        for (Object content : views) {
            ViewImpl state = new ViewImpl(this, (Element) content);
            myViews.put(state.getID(), state);
        }

        XmlSerializer.deserializeInto(myGeneral, parentNode.getChild(StringUtil.getShortName(myGeneral.getClass().getName(), '$')));

        return parentNode;
    }

    
    public Element write(Element parentNode) {
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

    
    public ViewImpl getStateFor(Content content) {
        return getOrCreateView(getOrCreateContentId(content));
    }

    public void clearStateFor(Content content) {
        String id = getOrCreateContentId(content);
        myDefaultViews.remove(id);
        ViewImpl view = myViews.remove(id);
        if (view != null) {
            Tab tab = view.getTab();
            if (tab instanceof TabImpl) {
                myTabs.remove(tab);
            }
        }
    }

    
    private static String getOrCreateContentId(Content content) {
        String id = content.getUserData(ViewImpl.ID);
        if (id == null) {
            id = "UnknownView-" + content.getDisplayName();
            content.putUserData(ViewImpl.ID, id);
        }
        return id;
    }

    
    private ViewImpl getOrCreateView(String id) {
        ViewImpl view = myViews.get(id);
        if (view == null) {
            view = getOrCreateDefault(id).createView(this);
            myViews.put(id, view);
        }
        return view;
    }

    
    private ViewImpl.Default getOrCreateDefault(String id) {
        if (myDefaultViews.containsKey(id)) {
            return myDefaultViews.get(id);
        }
        return setDefault(id, Integer.MAX_VALUE, PlaceInGrid.bottom, false);
    }

    
    public TabImpl.Default setDefault(int tabID, String displayName, Image icon) {
        TabImpl.Default tab = new TabImpl.Default(tabID, displayName, icon);
        myDefaultTabs.put(tabID, tab);
        return tab;
    }

    
    public ViewImpl.Default setDefault(String id, int tabIndex, PlaceInGrid placeInGrid, boolean isMinimized) {
        ViewImpl.Default view = new ViewImpl.Default(id, tabIndex, placeInGrid, isMinimized);
        myDefaultViews.put(id, view);
        return view;
    }

    
    public PlaceInGrid getDefaultGridPlace(Content content) {
        return getOrCreateDefault(getOrCreateContentId(content)).getPlaceInGrid();
    }

    public boolean isToFocus(String id, String condition) {
        return Comparing.equal(id, getToFocus(condition));
    }

    public void setToFocus(String id, String condition) {
        myGeneral.focusOnCondition.put(condition, id);
    }

    public void setDefaultToFocus(String id, String condition, LayoutAttractionPolicy policy) {
        myDefaultFocus.put(condition, Pair.create(id, policy));
    }

    public @Nullable String getToFocus(String condition) {
        return myGeneral.focusOnCondition.containsKey(condition) ? myGeneral.focusOnCondition.get(condition) :
            myDefaultFocus.containsKey(condition) ? myDefaultFocus.get(condition).getFirst() : null;
    }

    
    public LayoutAttractionPolicy getAttractionPolicy(String condition) {
        Pair<String, LayoutAttractionPolicy> pair = myDefaultFocus.get(condition);
        return pair == null ? new LayoutAttractionPolicy.FocusOnce() : pair.getSecond();
    }

    /**
     * States of contents marked as "lightweight" won't be persisted
     */
    public void setLightWeight(Content content) {
        if (myLightWeightIds == null) {
            myLightWeightIds = new HashSet<>();
        }
        myLightWeightIds.add(getOrCreateContentId(content));
    }

    @Override
    public boolean isTabLabelsHidden() {
        return myGeneral.isTabLabelsHidden;
    }

    @Override
    public void setTabLabelsHidden(boolean tabLabelsHidden) {
        myGeneral.isTabLabelsHidden = tabLabelsHidden;
    }

    public static class General {
        public volatile boolean horizontalToolbar = false;
        public volatile Map<String, String> focusOnCondition = new HashMap<>();
        public volatile boolean isTabLabelsHidden = true;
    }
}
