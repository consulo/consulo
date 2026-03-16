// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.editor.internal;

import consulo.codeEditor.EditorEx;
import consulo.codeEditor.RealEditor;
import consulo.codeEditor.event.*;
import consulo.codeEditor.internal.EditorEventMulticasterEx;
import consulo.codeEditor.internal.ErrorStripeListener;
import consulo.codeEditor.internal.ErrorStripeMarkupModel;
import consulo.component.extension.ExtensionPointName;
import consulo.disposer.Disposable;
import consulo.document.Document;
import consulo.document.event.DocumentEvent;
import consulo.document.event.DocumentListener;
import consulo.document.internal.DocumentEx;
import consulo.document.internal.EditReadOnlyListener;
import consulo.document.internal.EditorDocumentPriorities;
import consulo.document.internal.PrioritizedDocumentListener;
import consulo.proxy.EventDispatcher;
import consulo.ui.annotation.RequiredUIAccess;
import kava.beans.PropertyChangeListener;
import org.jetbrains.annotations.TestOnly;

import java.util.*;

public class EditorEventMulticasterImpl implements EditorEventMulticasterEx {
  private static final ExtensionPointName<EditorMouseListener> MOUSE_EP = ExtensionPointName.create(EditorMouseListener.class);
  private static final ExtensionPointName<EditorMouseMotionListener> MOUSE_MOTION_EP = ExtensionPointName.create(EditorMouseMotionListener.class);
  private static final ExtensionPointName<EditorDocumentListener> DOCUMENT_EP = ExtensionPointName.create(EditorDocumentListener.class);

  private final EventDispatcher<DocumentListener> myDocumentMulticaster = EventDispatcher.create(DocumentListener.class);
  private final EventDispatcher<PrioritizedDocumentListener> myPrioritizedDocumentMulticaster =
          EventDispatcher.create(PrioritizedDocumentListener.class, Collections.singletonMap("getPriority", EditorDocumentPriorities.RANGE_MARKER));
  private final EventDispatcher<EditReadOnlyListener> myEditReadOnlyMulticaster = EventDispatcher.create(EditReadOnlyListener.class);

  private final EventDispatcher<EditorMouseListener> myEditorMouseMulticaster = EventDispatcher.create(EditorMouseListener.class);
  private final EventDispatcher<EditorMouseMotionListener> myEditorMouseMotionMulticaster = EventDispatcher.create(EditorMouseMotionListener.class);
  private final EventDispatcher<ErrorStripeListener> myErrorStripeMulticaster = EventDispatcher.create(ErrorStripeListener.class);
  private final EventDispatcher<CaretListener> myCaretMulticaster = EventDispatcher.create(CaretListener.class);
  private final EventDispatcher<SelectionListener> mySelectionMulticaster = EventDispatcher.create(SelectionListener.class);
  private final EventDispatcher<VisibleAreaListener> myVisibleAreaMulticaster = EventDispatcher.create(VisibleAreaListener.class);
  private final EventDispatcher<PropertyChangeListener> myPropertyChangeMulticaster = EventDispatcher.create(PropertyChangeListener.class);
  private final EventDispatcher<FocusChangeListener> myFocusChangeListenerMulticaster = EventDispatcher.create(FocusChangeListener.class);

  public void registerDocument(DocumentEx document) {
    document.addDocumentListener(myDocumentMulticaster.getMulticaster());
    document.addDocumentListener(new DocumentListener() {
      @Override
      public void beforeDocumentChange(DocumentEvent event) {
        DOCUMENT_EP.forEachExtensionSafe(it -> it.beforeDocumentChange(event));
      }

      @Override
      public void documentChanged(DocumentEvent event) {
        DOCUMENT_EP.forEachExtensionSafe(it -> it.documentChanged(event));
      }

      @Override
      public void bulkUpdateStarting(Document document) {
        DOCUMENT_EP.forEachExtensionSafe(it -> it.bulkUpdateStarting(document));
      }

      @Override
      public void bulkUpdateFinished(Document document) {
        DOCUMENT_EP.forEachExtensionSafe(it -> it.bulkUpdateFinished(document));
      }
    });
    document.addDocumentListener(myPrioritizedDocumentMulticaster.getMulticaster());
    document.addEditReadOnlyListener(myEditReadOnlyMulticaster.getMulticaster());
  }

  public void registerEditor(EditorEx editor) {
    editor.addEditorMouseListener(myEditorMouseMulticaster.getMulticaster());
    editor.addEditorMouseListener(new EditorMouseListener() {
      @Override
      public void mousePressed(EditorMouseEvent event) {
        MOUSE_EP.forEachExtensionSafe(it -> it.mousePressed(event));
      }

      @Override
      public void mouseClicked(EditorMouseEvent event) {
        MOUSE_EP.forEachExtensionSafe(it -> it.mouseClicked(event));
      }

      @Override
      public void mouseReleased(EditorMouseEvent event) {
        MOUSE_EP.forEachExtensionSafe(it -> it.mouseReleased(event));
      }

      @Override
      public void mouseEntered(EditorMouseEvent event) {
        MOUSE_EP.forEachExtensionSafe(it -> it.mouseEntered(event));
      }

      @Override
      public void mouseExited(EditorMouseEvent event) {
        MOUSE_EP.forEachExtensionSafe(it -> it.mouseExited(event));
      }
    });

    editor.addEditorMouseMotionListener(myEditorMouseMotionMulticaster.getMulticaster());
    editor.addEditorMouseMotionListener(new EditorMouseMotionListener() {
      @RequiredUIAccess
      @Override
      public void mouseMoved(EditorMouseEvent event) {
        MOUSE_MOTION_EP.forEachExtensionSafe(it -> it.mouseMoved(event));
      }

      @RequiredUIAccess
      @Override
      public void mouseDragged(EditorMouseEvent event) {
        MOUSE_MOTION_EP.forEachExtensionSafe(it -> it.mouseDragged(event));
      }
    });

    ((ErrorStripeMarkupModel)editor.getMarkupModel()).addErrorMarkerListener(myErrorStripeMulticaster.getMulticaster(), ((RealEditor)editor).getDisposable());
    editor.getCaretModel().addCaretListener(myCaretMulticaster.getMulticaster());
    editor.getSelectionModel().addSelectionListener(mySelectionMulticaster.getMulticaster());
    editor.getScrollingModel().addVisibleAreaListener(myVisibleAreaMulticaster.getMulticaster());
    editor.addPropertyChangeListener(myPropertyChangeMulticaster.getMulticaster());
    editor.addFocusListener(myFocusChangeListenerMulticaster.getMulticaster());
  }

  @Override
  public void addDocumentListener(DocumentListener listener) {
    myDocumentMulticaster.addListener(listener);
  }

  @Override
  public void addDocumentListener(DocumentListener listener, Disposable parentDisposable) {
    myDocumentMulticaster.addListener(listener, parentDisposable);
  }

  /**
   * Dangerous method. When high priority listener fires the underlying subsystems (e.g. folding,caret, etc) may not be ready yet.
   * So all requests to the e.g. caret offset might generate exceptions.
   * Use for internal purposes only.
   *
   * @see EditorDocumentPriorities
   */
  public void addPrioritizedDocumentListener(PrioritizedDocumentListener listener, Disposable parent) {
    myPrioritizedDocumentMulticaster.addListener(listener, parent);
  }

  @Override
  public void removeDocumentListener(DocumentListener listener) {
    myDocumentMulticaster.removeListener(listener);
  }

  @Override
  public void addEditorMouseListener(EditorMouseListener listener) {
    myEditorMouseMulticaster.addListener(listener);
  }

  @Override
  public void addEditorMouseListener(EditorMouseListener listener, Disposable parentDisposable) {
    myEditorMouseMulticaster.addListener(listener, parentDisposable);
  }

  @Override
  public void removeEditorMouseListener(EditorMouseListener listener) {
    myEditorMouseMulticaster.removeListener(listener);
  }

  @Override
  public void addEditorMouseMotionListener(EditorMouseMotionListener listener) {
    myEditorMouseMotionMulticaster.addListener(listener);
  }

  @Override
  public void addEditorMouseMotionListener(EditorMouseMotionListener listener, Disposable parentDisposable) {
    myEditorMouseMotionMulticaster.addListener(listener, parentDisposable);
  }

  @Override
  public void removeEditorMouseMotionListener(EditorMouseMotionListener listener) {
    myEditorMouseMotionMulticaster.removeListener(listener);
  }

  @Override
  public void addCaretListener(CaretListener listener) {
    myCaretMulticaster.addListener(listener);
  }

  @Override
  public void addCaretListener(CaretListener listener, Disposable parentDisposable) {
    myCaretMulticaster.addListener(listener, parentDisposable);
  }

  @Override
  public void removeCaretListener(CaretListener listener) {
    myCaretMulticaster.removeListener(listener);
  }

  @Override
  public void addSelectionListener(SelectionListener listener) {
    mySelectionMulticaster.addListener(listener);
  }

  @Override
  public void addSelectionListener(SelectionListener listener, Disposable parentDisposable) {
    mySelectionMulticaster.addListener(listener, parentDisposable);
  }

  @Override
  public void removeSelectionListener(SelectionListener listener) {
    mySelectionMulticaster.removeListener(listener);
  }

  @Override
  public void addErrorStripeListener(ErrorStripeListener listener, Disposable parentDisposable) {
    myErrorStripeMulticaster.addListener(listener, parentDisposable);
  }

  @Override
  public void addVisibleAreaListener(VisibleAreaListener listener) {
    myVisibleAreaMulticaster.addListener(listener);
  }

  @Override
  public void addVisibleAreaListener(VisibleAreaListener listener, Disposable parent) {
    myVisibleAreaMulticaster.addListener(listener, parent);
  }

  @Override
  public void removeVisibleAreaListener(VisibleAreaListener listener) {
    myVisibleAreaMulticaster.removeListener(listener);
  }

  @Override
  public void addEditReadOnlyListener(EditReadOnlyListener listener, Disposable parentDisposable) {
    myEditReadOnlyMulticaster.addListener(listener, parentDisposable);
  }

  @Override
  public void addPropertyChangeListener(PropertyChangeListener listener, Disposable parentDisposable) {
    myPropertyChangeMulticaster.addListener(listener, parentDisposable);
  }

  @Override
  public void addFocusChangeListener(FocusChangeListener listener, Disposable parentDisposable) {
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
