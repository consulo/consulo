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

package consulo.compiler.setting;

import consulo.annotation.DeprecationInfo;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.util.io.FileUtil;
import consulo.util.xml.serializer.SerializationFilter;
import consulo.util.xml.serializer.SkipDefaultValuesSerializationFilters;
import consulo.util.xml.serializer.XmlSerializer;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import org.jdom.Element;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashSet;

/**
 * @author nik
 */
public class ExcludedEntriesConfiguration implements Disposable {
    private static final SerializationFilter ourSerializationFilter = new SkipDefaultValuesSerializationFilters();

    private final Collection<ExcludeEntryDescription> myExcludeEntryDescriptions = new LinkedHashSet<>();
    private ExcludeEntryDescription[] myCachedDescriptions = null;

    public synchronized boolean isEmpty() {
        return myExcludeEntryDescriptions.isEmpty();
    }

    public synchronized ExcludeEntryDescription[] getExcludeEntryDescriptions() {
        if (myCachedDescriptions == null) {
            myCachedDescriptions = myExcludeEntryDescriptions.toArray(new ExcludeEntryDescription[myExcludeEntryDescriptions.size()]);
        }
        return myCachedDescriptions;
    }

    public synchronized void addExcludeEntryDescription(ExcludeEntryDescription description) {
        myExcludeEntryDescriptions.add(description);
        myCachedDescriptions = null;
    }

    public synchronized void removeExcludeEntryDescription(ExcludeEntryDescription description) {
        myExcludeEntryDescriptions.remove(description);
        myCachedDescriptions = null;
    }

    public synchronized void removeAllExcludeEntryDescriptions() {
        myExcludeEntryDescriptions.clear();
        myCachedDescriptions = null;
    }

    public synchronized boolean containsExcludeEntryDescription(ExcludeEntryDescription description) {
        return myExcludeEntryDescriptions.contains(description);
    }

    public boolean isExcluded(Path file) {
        String filePath = FileUtil.toSystemIndependentName(file.toString());
        for (ExcludeEntryDescription entryDescription : getExcludeEntryDescriptions()) {
            String descriptionPath = VirtualFileUtil.urlToPath(entryDescription.getUrl());
            if (entryDescription.isFile()) {
                if (FileUtil.pathsEqual(descriptionPath, filePath)) {
                    return true;
                }
            }
            else if (entryDescription.isIncludeSubdirectories()) {
                if (FileUtil.isAncestor(descriptionPath, filePath, false)) {
                    return true;
                }
            }
            else {
                if (Files.isDirectory(file)) {
                    continue;
                }
                Path parent = file.getParent();
                if (parent != null && FileUtil.pathsEqual(descriptionPath, FileUtil.toSystemIndependentName(parent.toString()))) {
                    return true;
                }
            }
        }
        return false;
    }

    @Deprecated
    @DeprecationInfo("Use #loadState(ExcludedEntriesConfigurationState)")
    public void readExternal(Element node) {
        ExcludedEntriesConfigurationState state = XmlSerializer.deserialize(node, ExcludedEntriesConfigurationState.class);
        if (state != null) {
            loadState(state);
        }
    }

    @Deprecated
    @DeprecationInfo("Use #getState()")
    public void writeExternal(Element element) {
        XmlSerializer.serializeInto(getState(), element, ourSerializationFilter);
    }

    @Override
    public void dispose() {
        for (ExcludeEntryDescription description : myExcludeEntryDescriptions) {
            Disposer.dispose(description);
        }
    }

    public ExcludedEntriesConfigurationState getState() {
        ExcludedEntriesConfigurationState state = new ExcludedEntriesConfigurationState();
        for (ExcludeEntryDescription description : getExcludeEntryDescriptions()) {
            if (description.isFile()) {
                ExcludeEntryFileState fileState = new ExcludeEntryFileState();
                fileState.url = description.getUrl();
                state.files.add(fileState);
            }
            else {
                ExcludeEntryDirectoryState directoryState = new ExcludeEntryDirectoryState();
                directoryState.url = description.getUrl();
                directoryState.includeSubdirectories = description.isIncludeSubdirectories();
                state.directories.add(directoryState);
            }
        }
        return state;
    }

    public void loadState(ExcludedEntriesConfigurationState state) {
        for (ExcludeEntryFileState fileState : state.files) {
            if (fileState.url != null) {
                addExcludeEntryDescription(new ExcludeEntryDescription(fileState.url, false, true, this));
            }
        }

        for (ExcludeEntryDirectoryState directoryState : state.directories) {
            if (directoryState.url != null) {
                addExcludeEntryDescription(
                    new ExcludeEntryDescription(directoryState.url, directoryState.includeSubdirectories, false, this)
                );
            }
        }
    }
}
