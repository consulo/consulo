/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.ide.impl.idea.find.impl;

import consulo.component.persist.PersistentStateComponent;
import consulo.util.collection.ArrayUtil;
import consulo.util.xml.serializer.XmlSerializerUtil;
import consulo.util.xml.serializer.annotation.AbstractCollection;
import consulo.util.xml.serializer.annotation.Property;
import consulo.util.xml.serializer.annotation.Tag;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public class FindInProjectSettingsBase implements PersistentStateComponent<FindInProjectSettingsBase> {
    private static final int MAX_RECENT_SIZE = 30;

    @Tag("findStrings")
    @Property(surroundWithTag = false)
    @AbstractCollection(surroundWithTag = false, elementTag = "find", elementValueAttribute = "")
    public List<String> findStrings = new ArrayList<>();

    @Tag("replaceStrings")
    @Property(surroundWithTag = false)
    @AbstractCollection(surroundWithTag = false, elementTag = "replace", elementValueAttribute = "")
    public List<String> replaceStrings = new ArrayList<>();

    @Tag("dirStrings")
    @Property(surroundWithTag = false)
    @AbstractCollection(surroundWithTag = false, elementTag = "dir", elementValueAttribute = "")
    public List<String> dirStrings = new ArrayList<>();

    @Override
    public void loadState(FindInProjectSettingsBase state) {
        XmlSerializerUtil.copyBean(state, this);
        //Avoid duplicates
        LinkedHashSet<String> tmp = new LinkedHashSet<>(findStrings);
        findStrings.clear();
        findStrings.addAll(tmp);

        tmp.clear();
        tmp.addAll(replaceStrings);
        replaceStrings.clear();
        replaceStrings.addAll(tmp);

        tmp.clear();
        tmp.addAll(dirStrings);
        dirStrings.clear();
        dirStrings.addAll(tmp);
    }

    @Override
    public FindInProjectSettingsBase getState() {
        return this;
    }

    public void addDirectory(@Nonnull String s) {
        if (s.isEmpty()) {
            return;
        }
        addRecentStringToList(s, dirStrings);
    }

    @Nonnull
    public List<String> getRecentDirectories() {
        return new ArrayList<>(dirStrings);
    }

    public void addStringToFind(@Nonnull String s) {
        if (s.indexOf('\r') >= 0 || s.indexOf('\n') >= 0) {
            return;
        }
        addRecentStringToList(s, findStrings);
    }

    public void addStringToReplace(@Nonnull String s) {
        if (s.indexOf('\r') >= 0 || s.indexOf('\n') >= 0) {
            return;
        }
        addRecentStringToList(s, replaceStrings);
    }

    @Nonnull
    public String[] getRecentFindStrings() {
        return ArrayUtil.toStringArray(findStrings);
    }

    @Nonnull
    public String[] getRecentReplaceStrings() {
        return ArrayUtil.toStringArray(replaceStrings);
    }

    static void addRecentStringToList(@Nonnull String str, @Nonnull List<String> list) {
        if (list.contains(str)) {
            list.remove(str);
        }
        list.add(str);
        while (list.size() > MAX_RECENT_SIZE) {
            list.remove(0);
        }
    }
}
