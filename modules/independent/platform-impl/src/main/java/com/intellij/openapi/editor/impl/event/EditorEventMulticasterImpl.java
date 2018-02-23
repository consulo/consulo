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
package com.intellij.openapi.editor.impl.event;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.event.*;
import com.intellij.openapi.editor.ex.*;
import com.intellij.openapi.editor.impl.EditorImpl;
import com.intellij.util.EventDispatcher;
import javax.annotation.Nonnull;
import org.jetbrains.annotations.TestOnly;

import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class EditorEventMulticasterImpl implements EditorEventMulticasterEx {
  private final EventDispatcher<DocumentListener> myDocumentMulticaster = EventDispatcher.create(DocumentListener.class);
  private final EventDispatcher<EditReadOnlyListener> myEditReadOnlyMulticaster = EventDispatcher.create(EditReadOnlyListener.class);

  private final EventDispatcher<EditorMouseListener> myEditorMouseMulticaster = EventDispatcher.create(EditorMouseListener.class);
  private final EventDispatcher<EditorMouseMotionListener> myEditorMouseMotionMulticaster = EventDispatcher.create(EditorMouseMotionListener.class);
  private final EventDispatcher<ErrorStripeListener> myErrorStripeMulticaster = EventDispatcher.create(ErrorStripeListener.class);
  private final EventDispatcher<CaretListener> myCaretMulticaster = EventDispatcher.create(CaretListener.class);
  private final EventDispatcher<SelectionListener> mySelectionMulticaster = EventDispatcher.create(SelectionListener.class);
  private final EventDispatcher<VisibleAreaListener> myVisibleAreaMulticaster = EventDispatcher.create(VisibleAreaListener.class);
  private final EventDispatcher<PropertyChangeListener> myPropertyChangeMulticaster = EventDispatcher.create(PropertyChangeListener.class);
  private final EventDispatcher<FocusChangeListener> myFocusChangeListenerMulticaster = EventDispatcher.create(FocusChangeListener.class);

  public void registerDocument(@Nonnull DocumentEx document) {
    document.addDocumentListener(myDocumentMulticaster.getMulticaster());
    document.addEditReadOnlyListener(myEditReadOnlyMulticaster.getMulticaster());
  }

  public void registerEditor(@Nonnull EditorEx editor) {
    editor.addEditorMouseListener(myEditorMouseMulticaster.getMulticaster());
    editor.addEditorMouseMotionListener(myEditorMouseMotionMulticaster.getMulticaster());
    ((EditorMarkupModel) editor.getMarkupModel()).addErrorMarkerListener(myErrorStripeMulticaster.getMulticaster(), ((EditorImpl)editor).getDisposable());
    editor.getCaretModel().addCaretListener(myCaretMulticaster.getMulticaster());
    editor.getSelectionModel().addSelectionListener(mySelectionMulticaster.getMulticaster());
    editor.getScrollingModel().addVisibleAreaListener(myVisibleAreaMulticaster.getMulticaster());
    editor.addPropertyChangeListener(myPropertyChangeMulticaster.getMulticaster());
    editor.addFocusListener(myFocusChangeListenerMulticaster.getMulticaster());
  }

  @Override
  public void addDocumentListener(@Nonnull DocumentListener listener) {
    myDocumentMulticaster.addListener(listener);
  }

  @Override
  public void addDocumentListener(@Nonnull DocumentListener listener, @Nonnull Disposable parentDisposable) {
    myDocumentMulticaster.addListener(listener, parentDisposable);
  }

  @Override
  public void removeDocumentListener(@Nonnull DocumentListener listener) {
    myDocumentMulticaster.removeListener(listener);
  }

  @Override
  public void addEditorMouseListener(@Nonnull EditorMouseListener listener) {
    myEditorMouseMulticaster.addListener(listener);
  }

  @Override
  public void addEditorMouseListener(@Nonnull EditorMouseListener listener, @Nonnull Disposable parentDisposable) {
    myEditorMouseMulticaster.addListener(listener, parentDisposable);
  }

  @Override
  public void removeEditorMouseListener(@Nonnull EditorMouseListener listener) {
    myEditorMouseMulticaster.removeListener(listener);
  }

  @Override
  public void addEditorMouseMotionListener(@Nonnull EditorMouseMotionListener listener) {
    myEditorMouseMotionMulticaster.addListener(listener);
  }

  @Override
  public void addEditorMouseMotionListener(@Nonnull EditorMouseMotionListener listener, @Nonnull Disposable parentDisposable) {
    myEditorMouseMotionMulticaster.addListener(listener, parentDisposable);
  }

  @Override
  public void removeEditorMouseMotionListener(@Nonnull EditorMouseMotionListener listener) {
    myEditorMouseMotionMulticaster.removeListener(listener);
  }

  @Override
  public void addCaretListener(@Nonnull CaretListener listener) {
    myCaretMulticaster.addListener(listener);
  }

  @Override
  public void addCaretListener(@Nonnull CaretListener listener, @Nonnull Disposable parentDisposable) {
    myCaretMulticaster.addListener(listener, parentDisposable);
  }

  @Override
  public void removeCaretListener(@Nonnull CaretListener listener) {
    myCaretMulticaster.removeListener(listener);
  }

  @Override
  public void addSelectionListener(@Nonnull SelectionListener listener) {
    mySelectionMulticaster.addListener(listener);
  }

  @Override
  public void addSelectionListener(@Nonnull SelectionListener listener, @Nonnull Disposable parentDisposable) {
    mySelectionMulticaster.addListener(listener, parentDisposable);
  }

  @Override
  public void removeSelectionListener(@Nonnull SelectionListener listener) {
    mySelectionMulticaster.removeListener(listener);
  }

  @Override
  public void addErrorStripeListener(@Nonnull ErrorStripeListener listener) {
    myErrorStripeMulticaster.addListener(listener);
  }

  @Override
  public void addErrorStripeListener(@Nonnull ErrorStripeListener listener, @Nonnull Disposable parentDisposable) {
    myErrorStripeMulticaster.addListener(listener, parentDisposable);
  }

  @Override
  public void removeErrorStripeListener(@Nonnull ErrorStripeListener listener) {
    myErrorStripeMulticaster.removeListener(listener);
  }

  @Override
  public void addVisibleAreaListener(@Nonnull VisibleAreaListener listener) {
    myVisibleAreaMulticaster.addListener(listener);
  }

  @Override
  public void removeVisibleAreaListener(@Nonnull VisibleAreaListener listener) {
    myVisibleAreaMulticaster.removeListener(listener);
  }

  @Override
  public void addEditReadOnlyListener(@Nonnull EditReadOnlyListener listener) {
    myEditReadOnlyMulticaster.addListener(listener);
  }

  @Override
  public void removeEditReadOnlyListener(@Nonnull EditReadOnlyListener listener) {
    myEditReadOnlyMulticaster.removeListener(listener);
  }

  @Override
  public void addPropertyChangeListener(@Nonnull PropertyChangeListener listener) {
    myPropertyChangeMulticaster.addListener(listener);
  }

  @Override
  public void removePropertyChangeListener(@Nonnull PropertyChangeListener listener) {
    myPropertyChangeMulticaster.removeListener(listener);
  }

  @Override
  public void addFocusChangeListner(@Nonnull FocusChangeListener listener) {
    myFocusChangeListenerMulticaster.addListener(listener);
  }

  @Override
  public void addFocusChangeListner(@Nonnull FocusChangeListener listener, @Nonnull Disposable parentDisposable) {
    myFocusChangeListenerMulticaster.addListener(listener,parentDisposable);
  }

  @Override
  public void removeFocusChangeListner(@Nonnull FocusChangeListener listener) {
    myFocusChangeListenerMulticaster.removeListener(listener);
  }

  @TestOnly
  public Map<Class, List> getListeners() {
    Map<Class, List> myCopy = new LinkedHashMap<>();
    myCopy.put(DocumentListener.class, new ArrayList<>(myDocumentMulticaster.getListeners()));
    myCopy.put(EditReadOnlyListener.class, new ArrayList<>(myEditReadOnlyMulticaster.getListeners()));

    myCopy.put(EditorMouseListener.class, new ArrayList<>(myEditorMouseMulticaster.getListeners()));
    myCopy.put(EditorMouseMotionListener.class, new ArrayList<>(myEditorMouseMotionMulticaster.getListeners()));
    myCopy.put(ErrorStripeListener.class, new ArrayList<>(myErrorStripeMulticaster.getListeners()));
    myCopy.put(CaretListener.class, new ArrayList<>(myCaretMulticaster.getListeners()));
    myCopy.put(SelectionListener.class, new ArrayList<>(mySelectionMulticaster.getListeners()));
    myCopy.put(VisibleAreaListener.class, new ArrayList<>(myVisibleAreaMulticaster.getListeners()));
    myCopy.put(PropertyChangeListener.class, new ArrayList<>(myPropertyChangeMulticaster.getListeners()));
    myCopy.put(FocusChangeListener.class, new ArrayList<>(myFocusChangeListenerMulticaster.getListeners()));
    return myCopy;
  }
}
