/*
 * Copyright 2013-2026 consulo.io
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
package consulo.it.internal.ui;

import consulo.logging.Logger;
import consulo.ui.image.IconLibrary;
import consulo.ui.image.IconLibraryManager;
import consulo.ui.style.StyleManager;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author VISTALL
 */
public class HeadlessIconLibraryManager implements IconLibraryManager {
    private static final Logger LOG = Logger.getInstance(HeadlessIconLibraryManager.class);

    private final Map<String, IconLibrary> myLibraries = new LinkedHashMap<>();
    private final AtomicLong myModificationCount = new AtomicLong();

    private volatile @Nullable String myActiveLibraryId;

    public HeadlessIconLibraryManager() {
        myLibraries.put(LIGHT_LIBRARY_ID, new HeadlessIconLibrary(LIGHT_LIBRARY_ID, false));
        myLibraries.put(DARK_LIBRARY_ID, new HeadlessIconLibrary(DARK_LIBRARY_ID, true));
    }

    @Override
    public Map<String, IconLibrary> getLibraries() {
        return Collections.unmodifiableMap(myLibraries);
    }

    @Override
    public String getActiveLibraryId() {
        String activeLibraryId = myActiveLibraryId;
        if (activeLibraryId != null) {
            return activeLibraryId;
        }
        return StyleManager.get().getCurrentStyle().getIconLibraryId();
    }

    @Override
    public IconLibrary getActiveLibrary() {
        IconLibrary library = myLibraries.get(getActiveLibraryId());
        return library != null ? library : myLibraries.get(LIGHT_LIBRARY_ID);
    }

    @Override
    public boolean isFromStyle() {
        return myActiveLibraryId == null;
    }

    @Override
    public void setActiveLibrary(@Nullable String iconLibraryId) {
        if (iconLibraryId != null && !myLibraries.containsKey(iconLibraryId)) {
            LOG.warn("Can't find icon library with id: " + iconLibraryId);
            return;
        }

        myActiveLibraryId = iconLibraryId;
        myModificationCount.incrementAndGet();
    }

    @Override
    public void setActiveLibraryFromActiveStyle() {
        setActiveLibrary(null);
    }

    @Override
    public long getModificationCount() {
        return myModificationCount.get();
    }
}
