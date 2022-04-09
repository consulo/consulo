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

package consulo.content.impl.internal.library;

import consulo.application.ApplicationManager;
import consulo.component.persist.PersistentStateComponent;
import consulo.content.base.BinariesOrderRootType;
import consulo.content.library.Library;
import consulo.content.library.LibraryTable;
import consulo.content.library.PersistentLibraryKind;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.logging.Logger;
import consulo.proxy.EventDispatcher;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.Comparing;
import consulo.util.xml.serializer.InvalidDataException;
import consulo.util.xml.serializer.WriteExternalException;
import org.jdom.Element;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public abstract class LibraryTableBase implements PersistentStateComponent<Element>, LibraryTable, Disposable {
  private static final Logger LOG = Logger.getInstance(LibraryTableBase.class);
  private final EventDispatcher<Listener> myDispatcher = EventDispatcher.create(Listener.class);
  private LibraryModel myModel = new LibraryModel();
  private boolean myFirstLoad = true;

  @Override
  public ModifiableModel getModifiableModel() {
    return new LibraryModel(myModel);
  }

  @Override
  public Element getState() {
    final Element element = new Element("state");
    try {
      myModel.writeExternal(element);
    }
    catch (WriteExternalException e) {
      LOG.error(e);
    }
    return element;
  }

  @Override
  public void loadState(final Element element) {
    try {
      if (myFirstLoad) {
        myModel.readExternal(element);
      }
      else {
        final LibraryModel model = new LibraryModel();
        model.readExternal(element);
        commit(model);
      }

      myFirstLoad = false;
    }
    catch (InvalidDataException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  @Nonnull
  public Library[] getLibraries() {
    return myModel.getLibraries();
  }

  @Override
  @Nonnull
  public Iterator<Library> getLibraryIterator() {
    return myModel.getLibraryIterator();
  }

  @Override
  public Library getLibraryByName(@Nonnull String name) {
    return myModel.getLibraryByName(name);
  }

  @Override
  public void addListener(Listener listener) {
    myDispatcher.addListener(listener);
  }

  @Override
  public void addListener(Listener listener, Disposable parentDisposable) {
    myDispatcher.addListener(listener, parentDisposable);
  }

  @Override
  public void removeListener(Listener listener) {
    myDispatcher.removeListener(listener);
  }

  private void fireLibraryAdded(Library library) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("fireLibraryAdded: " + library);
    }
    myDispatcher.getMulticaster().afterLibraryAdded(library);
  }

  private void fireBeforeLibraryRemoved(Library library) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("fireBeforeLibraryRemoved: " + library);
    }
    myDispatcher.getMulticaster().beforeLibraryRemoved(library);
  }

  @Override
  public void dispose() {
    for (Library library : getLibraries()) {
      Disposer.dispose(library);
    }
  }

  @Override
  public Library createLibrary() {
    ApplicationManager.getApplication().assertWriteAccessAllowed();
    return createLibrary(null);
  }

  public void fireLibraryRenamed(@Nonnull Library library) {
    myDispatcher.getMulticaster().afterLibraryRenamed(library);
  }

  @Override
  public Library createLibrary(String name) {
    final ModifiableModel modifiableModel = getModifiableModel();
    final Library library = modifiableModel.createLibrary(name);
    modifiableModel.commit();
    return library;
  }

  @Override
  public void removeLibrary(@Nonnull Library library) {
    final ModifiableModel modifiableModel = getModifiableModel();
    modifiableModel.removeLibrary(library);
    modifiableModel.commit();
  }

  private void commit(LibraryModel model) {
    ApplicationManager.getApplication().assertWriteAccessAllowed();
    //todo[nik] remove LibraryImpl#equals method instead of using identity sets
    Set<Library> addedLibraries = ContainerUtil.newIdentityTroveSet(model.myLibraries);
    addedLibraries.removeAll(myModel.myLibraries);
    Set<Library> removedLibraries = ContainerUtil.newIdentityTroveSet(myModel.myLibraries);
    removedLibraries.removeAll(model.myLibraries);

    for (Library library : removedLibraries) {
      fireBeforeLibraryRemoved(library);
    }

    myModel = model;
    for (Library library : removedLibraries) {
      Disposer.dispose(library);
      fireAfterLibraryRemoved(library);
    }
    for (Library library : addedLibraries) {
      fireLibraryAdded(library);
    }
  }

  private void fireAfterLibraryRemoved(Library library) {
    myDispatcher.getMulticaster().afterLibraryRemoved(library);
  }

  public void readExternal(final Element element) throws InvalidDataException {
    myModel = new LibraryModel();
    myModel.readExternal(element);
  }

  public void writeExternal(final Element element) throws WriteExternalException {
    myModel.writeExternal(element);
  }

  public interface ModifiableModelEx extends ModifiableModel {
    Library createLibrary(String name, @Nullable PersistentLibraryKind type);
  }

  public class LibraryModel implements ModifiableModelEx {
    private final ArrayList<Library> myLibraries = new ArrayList<>();
    private boolean myWritable;

    private LibraryModel() {
      myWritable = false;
    }

    private LibraryModel(LibraryModel that) {
      myWritable = true;
      myLibraries.addAll(that.myLibraries);
    }

    @Override
    public void commit() {
      myWritable = false;
      LibraryTableBase.this.commit(this);
    }

    @Override
    @Nonnull
    public Iterator<Library> getLibraryIterator() {
      return Collections.unmodifiableList(myLibraries).iterator();
    }

    @Override
    @Nullable
    public Library getLibraryByName(@Nonnull String name) {
      for (Library myLibrary : myLibraries) {
        LibraryImpl library = (LibraryImpl)myLibrary;
        if (Comparing.equal(name, library.getName())) return library;
      }
      final String libraryPrefix = "library.";
      final String libPath = System.getProperty(libraryPrefix + name);
      if (libPath != null) {
        final LibraryImpl library = new LibraryImpl(name, null, LibraryTableBase.this, null);
        library.addRoot(libPath, BinariesOrderRootType.getInstance());
        return library;
      }
      return null;
    }


    @Override
    @Nonnull
    public Library[] getLibraries() {
      return myLibraries.toArray(new Library[myLibraries.size()]);
    }

    private void assertWritable() {
      LOG.assertTrue(myWritable);
    }

    @Override
    public Library createLibrary(String name) {
      return createLibrary(name, null);
    }

    @Override
    public Library createLibrary(String name, @Nullable PersistentLibraryKind kind) {
      assertWritable();
      final LibraryImpl library = new LibraryImpl(name, kind, LibraryTableBase.this, null);
      myLibraries.add(library);
      return library;
    }

    @Override
    public void removeLibrary(@Nonnull Library library) {
      assertWritable();
      myLibraries.remove(library);
    }

    @Override
    public boolean isChanged() {
      if (!myWritable) return false;
      Set<Library> thisLibraries = new HashSet<>(myLibraries);
      Set<Library> thatLibraries = new HashSet<>(myModel.myLibraries);
      return !thisLibraries.equals(thatLibraries);
    }

    public void readExternal(Element element) {
      HashMap<String, Library> libraries = new HashMap<>();
      for (Library library : myLibraries) {
        libraries.put(library.getName(), library);
      }

      final List libraryElements = element.getChildren(LibraryImpl.ELEMENT);
      for (Object libraryElement1 : libraryElements) {
        Element libraryElement = (Element)libraryElement1;
        final LibraryImpl library = new LibraryImpl(LibraryTableBase.this, libraryElement, null);
        if (library.getName() != null) {
          Library oldLibrary = libraries.get(library.getName());
          if (oldLibrary != null) {
            removeLibrary(oldLibrary);
          }

          myLibraries.add(library);
          fireLibraryAdded(library);
        }
        else {
          Disposer.dispose(library);
        }
      }
    }

    public void writeExternal(Element element) throws WriteExternalException {
      final List<Library> libraries = ContainerUtil.findAll(myLibraries, library -> !((LibraryEx)library).isDisposed());

      // todo: do not sort if project is directory-based
      ContainerUtil.sort(libraries, (o1, o2) -> o1.getName().toLowerCase().compareTo(o2.getName().toLowerCase()));

      for (final Library library : libraries) {
        if (library.getName() != null) {
          library.writeExternal(element);
        }
      }
    }
  }
}
