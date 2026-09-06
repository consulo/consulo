// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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
package consulo.language.index.impl.internal;

import consulo.content.bundle.Sdk;
import consulo.content.library.Library;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.module.content.internal.BuildableRootsChangeRescanningInfo;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.project.RootsChangeRescanningInfo;
import consulo.util.collection.SmartHashSet;
import consulo.util.collection.SmartList;

import java.util.List;
import java.util.Set;

public final class BuildableRootsChangeRescanningInfoImpl extends BuildableRootsChangeRescanningInfo {
    private static final Logger LOG = Logger.getInstance(BuildableRootsChangeRescanningInfoImpl.class);

    private final Set<Module> myModules = new SmartHashSet<>();
    private boolean myHasInheritedSdk;
    private final List<Sdk> mySdks = new SmartList<>();
    private final List<Library> myLibraries = new SmartList<>();
    private final List<OrderEntry> myOrderEntries = new SmartList<>();

    @Override
    public BuildableRootsChangeRescanningInfo addModule(Module module) {
        myModules.add(module);
        return this;
    }

    @Override
    public BuildableRootsChangeRescanningInfo addInheritedSdk() {
        LOG.debug("addInheritedSdk() is ignored: there is no project-level inherited sdk");
        myHasInheritedSdk = true;
        return this;
    }

    @Override
    public BuildableRootsChangeRescanningInfo addSdk(Sdk sdk) {
        mySdks.add(sdk);
        return this;
    }

    @Override
    public BuildableRootsChangeRescanningInfo addLibrary(Library library) {
        myLibraries.add(library);
        return this;
    }

    @Override
    public BuildableRootsChangeRescanningInfo addOrderEntry(OrderEntry orderEntry) {
        myOrderEntries.add(orderEntry);
        return this;
    }

    @Override
    public RootsChangeRescanningInfo buildInfo() {
        return new BuiltRescanningInfo(Set.copyOf(myModules), myHasInheritedSdk, List.copyOf(mySdks), List.copyOf(myLibraries),
            List.copyOf(myOrderEntries));
    }

    record BuiltRescanningInfo(Set<Module> modules,
                               boolean hasInheritedSdk,
                               List<Sdk> sdks,
                               List<Library> libraries,
                               List<OrderEntry> orderEntries)
        implements RootsChangeRescanningInfo {
    }
}
