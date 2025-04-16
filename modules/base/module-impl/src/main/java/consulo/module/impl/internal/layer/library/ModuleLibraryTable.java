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

package consulo.module.impl.internal.layer.library;

import consulo.application.content.impl.internal.library.LibraryTableBase;
import consulo.content.library.Library;
import consulo.content.library.LibraryTable;
import consulo.content.library.LibraryTablePresentation;
import consulo.content.library.PersistentLibraryKind;
import consulo.disposer.Disposable;
import consulo.module.Module;
import consulo.module.content.layer.orderEntry.LibraryOrderEntry;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.module.content.library.ModuleLibraryTablePresentation;
import consulo.module.impl.internal.layer.ModuleRootLayerImpl;
import consulo.module.impl.internal.layer.orderEntry.ModuleLibraryOrderEntryImpl;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.ConvertingIterator;
import consulo.util.collection.FilteringIterator;
import consulo.util.lang.function.Condition;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.function.Function;

/**
 * @author dsl
 */
public class ModuleLibraryTable implements LibraryTable, LibraryTableBase.ModifiableModelEx {
    private static final ModuleLibraryOrderEntryCondition MODULE_LIBRARY_ORDER_ENTRY_FILTER = new ModuleLibraryOrderEntryCondition();
    private static final OrderEntryToLibraryConvertor ORDER_ENTRY_TO_LIBRARY_CONVERTOR = new OrderEntryToLibraryConvertor();
    private final ModuleRootLayerImpl myRootLayer;

    public ModuleLibraryTable(ModuleRootLayerImpl rootLayer) {
        myRootLayer = rootLayer;
    }

    @Override
    @Nonnull
    public Library[] getLibraries() {
        final ArrayList<Library> result = new ArrayList<Library>();
        final Iterator<Library> libraryIterator = getLibraryIterator();
        ContainerUtil.addAll(result, libraryIterator);
        return result.toArray(new Library[result.size()]);
    }

    @Override
    public Library createLibrary() {
        return createLibrary(null);
    }

    @Override
    public Library createLibrary(String name) {
        return createLibrary(name, null);
    }

    @Override
    public Library createLibrary(String name, @Nullable PersistentLibraryKind kind) {
        final ModuleLibraryOrderEntryImpl orderEntry = new ModuleLibraryOrderEntryImpl(name, kind, myRootLayer);
        myRootLayer.addOrderEntry(orderEntry);
        return orderEntry.getLibrary();
    }

    @Override
    public void removeLibrary(@Nonnull Library library) {
        final Iterator<OrderEntry> orderIterator = myRootLayer.getOrderIterator();
        while (orderIterator.hasNext()) {
            OrderEntry orderEntry = orderIterator.next();
            if (orderEntry instanceof LibraryOrderEntry) {
                final LibraryOrderEntry libraryOrderEntry = (LibraryOrderEntry)orderEntry;
                if (libraryOrderEntry.isModuleLevel()) {
                    if (library.equals(libraryOrderEntry.getLibrary())) {
                        myRootLayer.removeOrderEntry(orderEntry);
                        //Disposer.dispose(library);
                        return;
                    }
                }
            }
        }
    }

    @Override
    @Nonnull
    public Iterator<Library> getLibraryIterator() {
        FilteringIterator<OrderEntry, LibraryOrderEntry> filteringIterator =
            new FilteringIterator<OrderEntry, LibraryOrderEntry>(myRootLayer.getOrderIterator(), MODULE_LIBRARY_ORDER_ENTRY_FILTER);
        return new ConvertingIterator<>(filteringIterator, ORDER_ENTRY_TO_LIBRARY_CONVERTOR);
    }

    @Override
    public String getTableLevel() {
        return LibraryTableImplUtil.MODULE_LEVEL;
    }

    @Override
    public LibraryTablePresentation getPresentation() {
        return ModuleLibraryTablePresentation.INSTANCE;
    }

    @Override
    public boolean isEditable() {
        return true;
    }

    @Override
    @Nullable
    public Library getLibraryByName(@Nonnull String name) {
        final Iterator<Library> libraryIterator = getLibraryIterator();
        while (libraryIterator.hasNext()) {
            Library library = libraryIterator.next();
            if (name.equals(library.getName())) {
                return library;
            }
        }
        return null;
    }

    @Override
    public void addListener(Listener listener) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addListener(Listener listener, Disposable parentDisposable) {
        throw new UnsupportedOperationException("Method addListener is not yet implemented in " + getClass().getName());
    }

    @Override
    public void removeListener(Listener listener) {
        throw new UnsupportedOperationException();
    }

    public Module getModule() {
        return myRootLayer.getModule();
    }

    private static class ModuleLibraryOrderEntryCondition implements Condition<OrderEntry> {
        @Override
        public boolean value(OrderEntry entry) {
            return entry instanceof LibraryOrderEntry && ((LibraryOrderEntry)entry).isModuleLevel() && ((LibraryOrderEntry)entry).getLibrary() != null;
        }
    }

    private static class OrderEntryToLibraryConvertor implements Function<LibraryOrderEntry, Library> {
        @Override
        public Library apply(LibraryOrderEntry o) {
            return o.getLibrary();
        }
    }

    @Override
    public void commit() {
    }

    @Override
    public boolean isChanged() {
        return myRootLayer.getRootModel().isChanged();
    }

    @Override
    public ModifiableModel getModifiableModel() {
        return this;
    }
}
