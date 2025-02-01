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
package consulo.container.internal.plugin;

import consulo.container.plugin.PluginId;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author VISTALL
 * @since 28-Aug-22
 */
public class ClassPathItem {
    private final List<ClassPathPluginSet> mySets;
    private final Set<String> myIndex;
    private final File myPath;

    public ClassPathItem(File path, List<ClassPathPluginSet> pluginSets, Set<String> index) {
        myPath = path;
        mySets = pluginSets;
        myIndex = index == null ? Collections.emptySet() : index;
    }

    public boolean accept(Set<PluginId> enabledPluginIds) {
        if (mySets.isEmpty()) {
            return true;
        }

        for (ClassPathPluginSet set : mySets) {
            if (set.accept(enabledPluginIds)) {
                return true;
            }
        }
        return false;
    }

    public File getPath() {
        return myPath;
    }

    public Set<String> getIndex() {
        return myIndex;
    }

    @Override
    public String toString() {
        return "ClassPathItem{" +
            "mySets=" + mySets +
            ", myIndex=" + myIndex +
            ", myPath=" + myPath +
            '}';
    }
}
