// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.editor.impl.event;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.event.*;
import com.intellij.openapi.editor.ex.*;
import com.intellij.openapi.editor.impl.EditorDocumentPriorities;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.util.EventDispatcher;
import consulo.disposer.Disposable;
import consulo.editor.internal.EditorInternal;
import consulo.ui.annotation.RequiredUIAccess;
import kava.beans.PropertyChangeListener;
import org.jetbrains.annotations.TestOnly;

import javax.annotation.Nonnull;
import java.util.*;

public class EditorEventMulticasterImpl implements EditorEventMulticasterEx {
  private static final ExtensionPointName<EditorMouseListener> MOUSE_EP = ExtensionPointName.create("com.intellij.editorFactoryMouseListener");
  private static final ExtensionPointName<EditorMouseMotionListener> MOUSE_MOTION_EP = ExtensionPointName.create("com.intellij.editorFactoryMouseMotionListener");
  private static final ExtensionPointName<DocumentListener> DOCUMENT_EP = ExtensionPointName.create("com.intellij.editorFactoryDocumentListener");

  private final EventDispatcher<DocumentListener> myDocumentMulticaster = EventDispatcher.create(DocumentListener.class);
  private final EventDispatcher<PrioritizedInternalDocumentListener> myPrioritizedDocumentMulticaster =
          EventDispatcher.create(PrioritizedInternalDocumentListener.class, Collections.singletonMap("getPriority", EditorDocumentPriorities.RANGE_MARKER));
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
    document.addDocumentListener(new DocumentListener() {
      @Override
      public void beforeDocumentChange(@Nonnull DocumentEvent event) {
        DOCUMENT_EP.forEachExtensionSafe(it -> it.beforeDocumentChange(event));
      }

      @Override
      public void documentChanged(@Nonnull DocumentEvent event) {
        DOCUMENT_EP.forEachExtensionSafe(it -> it.documentChanged(event));
      }

      @Override
      public void bulkUpdateStarting(@Nonnull Document document) {
        DOCUMENT_EP.forEachExtensionSafe(it -> it.bulkUpdateStarting(document));
      }

      @Override
      public void bulkUpdateFinished(@Nonnull Document document) {
        DOCUMENT_EP.forEachExtensionSafe(it -> it.bulkUpdateFinished(document));
      }
    });
    document.addDocumentListener(myPrioritizedDocumentMulticaster.getMulticaster());
    document.addEditReadOnlyListener(myEditReadOnlyMulticaster.getMulticaster());
  }

  public void registerEditor(@Nonnull EditorEx editor) {
    editor.addEditorMouseListener(myEditorMouseMulticaster.getMulticaster());
    editor.addEditorMouseListener(new EditorMouseListener() {
      @Override
      public void mousePressed(@Nonnull EditorMouseEvent event) {
        MOUSE_EP.forEachExtensionSafe(it -> it.mousePressed(event));
      }

      @Override
      public void mouseClicked(@Nonnull EditorMouseEvent event) {
        MOUSE_EP.forEachExtensionSafe(it -> it.mouseClicked(event));
      }

      @Override
      public void mouseReleased(@Nonnull EditorMouseEvent event) {
        MOUSE_EP.forEachExtensionSafe(it -> it.mouseReleased(event));
      }

      @Override
      public void mouseEntered(@Nonnull EditorMouseEvent event) {
        MOUSE_EP.forEachExtensionSafe(it -> it.mouseEntered(event));
      }

      @Override
      public void mouseExited(@Nonnull EditorMouseEvent event) {
        MOUSE_EP.forEachExtensionSafe(it -> it.mouseExited(event));
      }
    });

    editor.addEditorMouseMotionListener(myEditorMouseMotionMulticaster.getMulticaster());
    editor.addEditorMouseMotionListener(new EditorMouseMotionListener() {
      @RequiredUIAccess
      @Override
      public void mouseMoved(@Nonnull EditorMouseEvent event) {
        MOUSE_MOTION_EP.forEachExtensionSafe(it -> it.mouseMoved(event));
      }

      @RequiredUIAccess
      @Override
      public void mouseDragged(@Nonnull EditorMouseEvent event) {
        MOUSE_MOTION_EP.forEachExtensionSafe(it -> it.mouseDragged(event));
      }
    });

    ((EditorMarkupModel)editor.getMarkupModel()).addErrorMarkerListener(myErrorStripeMulticaster.getMulticaster(), ((EditorInternal)editor).getDisposable());
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

  /**
   * Dangerous method. When high priority listener fires the underlying subsystems (e.g. folding,caret, etc) may not be ready yet.
   * So all requests to the e.g. caret offset might generate exceptions.
   * Use for internal purposes only.
   *
   * @see EditorDocumentPriorities
   */
  public void addPrioritizedDocumentListener(@Nonnull PrioritizedInternalDocumentListener listener, @Nonnull Disposable parent) {
    myPrioritizedDocumentMulticaster.addListener(listener, parent);
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
  public void addErrorStripeListener(@Nonnull ErrorStripeListener listener, @Nonnull Disposable parentDisposable) {
    myErrorStripeMulticaster.addListener(listener, parentDisposable);
  }

  @Override
  public void addVisibleAreaListener(@Nonnull VisibleAreaListener listener) {
    myVisibleAreaMulticaster.addListener(listener);
  }

  @Override
  public void addVisibleAreaListener(@Nonnull VisibleAreaListener listener, @Nonnull Disposable parent) {
    myVisibleAreaMulticaster.addListener(listener, parent);
  }

  @Override
  public void removeVisibleAreaListener(@Nonnull VisibleAreaListener listener) {
    myVisibleAreaMulticaster.removeListener(listener);
  }

  @Override
  public void addEditReadOnlyListener(@Nonnull EditReadOnlyListener listener, @Nonnull Disposable parentDisposable) {
    myEditReadOnlyMulticaster.addListener(listener, parentDisposable);
  }

  @Override
  public void addPropertyChangeListener(@Nonnull PropertyChangeListener listener, @Nonnull Disposable parentDisposable) {
    myPropertyChangeMulticaster.addListener(listener, parentDisposable);
  }

  @Override
  public void addFocusChangeListener(@Nonnull FocusChangeListener listener, @Nonnull Disposable parentDisposable) {
    myFocusChangeListenerMulticaster.addListener(listener, parentDisposable);
  }

  @TestOnly
  public Map<Class<? extends EventListener>, List<? extends EventListener>> getListeners() {
    Map<Class<? extends EventListener>, List<? extends EventListener>> myCopy = new LinkedHashMap<>();
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
