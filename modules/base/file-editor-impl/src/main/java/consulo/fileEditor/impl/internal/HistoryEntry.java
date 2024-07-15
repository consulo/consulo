/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.fileEditor.impl.internal;

import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.fileEditor.FileEditorProvider;
import consulo.fileEditor.FileEditorProviderManager;
import consulo.fileEditor.FileEditorState;
import consulo.project.Project;
import consulo.util.lang.Pair;
import consulo.util.xml.serializer.InvalidDataException;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import consulo.virtualFileSystem.pointer.LightFilePointer;
import consulo.virtualFileSystem.pointer.VirtualFilePointer;
import consulo.virtualFileSystem.pointer.VirtualFilePointerManager;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jdom.Element;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * `Heavy` entries should be disposed with {@link #destroy()} to prevent leak of VirtualFilePointer
 */
public final class HistoryEntry {
  public static final String TAG = "entry";
  public static final String FILE_ATTR = "file";
  private static final String PROVIDER_ELEMENT = "provider";
  private static final String EDITOR_TYPE_ID_ATTR = "editor-type-id";
  private static final String SELECTED_ATTR_VALUE = "selected";
  private static final String STATE_ELEMENT = "state";

  @Nonnull
  private final VirtualFilePointer myFilePointer;
  /**
   * can be null when read from XML
   */
  @Nullable
  private FileEditorProvider mySelectedProvider;
  @Nonnull
  private final HashMap<FileEditorProvider, FileEditorState> myProvider2State;

  @Nullable
  private final Disposable myDisposable;

  private HistoryEntry(@Nonnull VirtualFilePointer filePointer,
                       @Nullable FileEditorProvider selectedProvider,
                       @Nullable Disposable disposable) {
    myFilePointer = filePointer;
    mySelectedProvider = selectedProvider;
    myDisposable = disposable;
    myProvider2State = new HashMap<>();
  }

  @Nonnull
  public static HistoryEntry createLight(@Nonnull VirtualFile file,
                                         @Nonnull FileEditorProvider[] providers,
                                         @Nonnull FileEditorState[] states,
                                         @Nonnull FileEditorProvider selectedProvider) {
    VirtualFilePointer pointer = new LightFilePointer(file);
    HistoryEntry entry = new HistoryEntry(pointer, selectedProvider, null);
    for (int i = 0; i < providers.length; i++) {
      entry.putState(providers[i], states[i]);
    }
    return entry;
  }

  @Nonnull
  public static HistoryEntry createLight(@Nonnull Project project, @Nonnull Element e) throws InvalidDataException {
    EntryData entryData = parseEntry(project, e);

    VirtualFilePointer pointer = new LightFilePointer(entryData.url);
    HistoryEntry entry = new HistoryEntry(pointer, entryData.selectedProvider, null);
    for (Pair<FileEditorProvider, FileEditorState> state : entryData.providerStates) {
      entry.putState(state.first, state.second);
    }
    return entry;
  }

  @Nonnull
  public static HistoryEntry createHeavy(@Nonnull Project project,
                                         @Nonnull VirtualFile file,
                                         @Nonnull FileEditorProvider[] providers,
                                         @Nonnull FileEditorState[] states,
                                         @Nonnull FileEditorProvider selectedProvider) {
    if (project.isDisposed()) return createLight(file, providers, states, selectedProvider);

    Disposable disposable = Disposable.newDisposable();
    VirtualFilePointer pointer = VirtualFilePointerManager.getInstance().create(file, disposable, null);

    HistoryEntry entry = new HistoryEntry(pointer, selectedProvider, disposable);
    for (int i = 0; i < providers.length; i++) {
      FileEditorProvider provider = providers[i];
      FileEditorState state = states[i];
      if (provider != null && state != null) {
        entry.putState(provider, state);
      }
    }
    return entry;
  }

  @Nonnull
  public static HistoryEntry createHeavy(@Nonnull Project project, @Nonnull Element e) throws InvalidDataException {
    if (project.isDisposed()) return createLight(project, e);

    EntryData entryData = parseEntry(project, e);

    Disposable disposable = Disposable.newDisposable();
    VirtualFilePointer pointer = VirtualFilePointerManager.getInstance().create(entryData.url, disposable, null);

    HistoryEntry entry = new HistoryEntry(pointer, entryData.selectedProvider, disposable);
    for (Pair<FileEditorProvider, FileEditorState> state : entryData.providerStates) {
      entry.putState(state.first, state.second);
    }
    return entry;
  }


  @Nonnull
  public VirtualFilePointer getFilePointer() {
    return myFilePointer;
  }

  @Nullable
  public VirtualFile getFile() {
    return myFilePointer.getFile();
  }

  public FileEditorState getState(@Nonnull FileEditorProvider provider) {
    return myProvider2State.get(provider);
  }

  public void putState(@Nonnull FileEditorProvider provider, @Nonnull FileEditorState state) {
    myProvider2State.put(provider, state);
  }

  @Nullable
  public FileEditorProvider getSelectedProvider() {
    return mySelectedProvider;
  }

  public void setSelectedProvider(@Nullable FileEditorProvider value) {
    mySelectedProvider = value;
  }

  public void destroy() {
    if (myDisposable != null) Disposer.dispose(myDisposable);
  }

  /**
   * @return element that was added to the <code>element</code>.
   * Returned element has tag {@link #TAG}. Never null.
   */
  public Element writeExternal(Element element, Project project) {
    Element e = new Element(TAG);
    element.addContent(e);
    e.setAttribute(FILE_ATTR, myFilePointer.getUrl());

    for (final Map.Entry<FileEditorProvider, FileEditorState> entry : myProvider2State.entrySet()) {
      FileEditorProvider provider = entry.getKey();

      Element providerElement = new Element(PROVIDER_ELEMENT);
      if (provider.equals(mySelectedProvider)) {
        providerElement.setAttribute(SELECTED_ATTR_VALUE, Boolean.TRUE.toString());
      }
      providerElement.setAttribute(EDITOR_TYPE_ID_ATTR, provider.getEditorTypeId());
      Element stateElement = new Element(STATE_ELEMENT);
      providerElement.addContent(stateElement);
      provider.writeState(entry.getValue(), project, stateElement);

      e.addContent(providerElement);
    }

    return e;
  }

  @Nonnull
  private static EntryData parseEntry(@Nonnull Project project, @Nonnull Element e) throws InvalidDataException {
    if (!e.getName().equals(TAG)) {
      throw new IllegalArgumentException("unexpected tag: " + e);
    }

    String url = e.getAttributeValue(FILE_ATTR);
    List<Pair<FileEditorProvider, FileEditorState>> providerStates = new ArrayList<>();
    FileEditorProvider selectedProvider = null;

    VirtualFile file = VirtualFileManager.getInstance().findFileByUrl(url);

    for (Element _e : e.getChildren(PROVIDER_ELEMENT)) {
      String typeId = _e.getAttributeValue(EDITOR_TYPE_ID_ATTR);
      FileEditorProvider provider = FileEditorProviderManager.getInstance().getProvider(typeId);
      if (provider == null) {
        continue;
      }
      if (Boolean.valueOf(_e.getAttributeValue(SELECTED_ATTR_VALUE))) {
        selectedProvider = provider;
      }

      Element stateElement = _e.getChild(STATE_ELEMENT);
      if (stateElement == null) {
        throw new InvalidDataException();
      }

      if (file != null) {
        FileEditorState state = provider.readState(stateElement, project, file);
        providerStates.add(Pair.create(provider, state));
      }
    }

    return new EntryData(url, providerStates, selectedProvider);
  }

  private static class EntryData {
    @Nonnull
    public final String url;
    @Nonnull
    public final List<Pair<FileEditorProvider, FileEditorState>> providerStates;
    @Nullable
    public final FileEditorProvider selectedProvider;

    public EntryData(@Nonnull String url,
                     @Nonnull List<Pair<FileEditorProvider, FileEditorState>> providerStates,
                     @Nullable FileEditorProvider selectedProvider) {
      this.url = url;
      this.providerStates = providerStates;
      this.selectedProvider = selectedProvider;
    }
  }
}
