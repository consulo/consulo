// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.desktop.awt.codeInsight.lookup;

import consulo.annotation.component.ServiceImpl;
import consulo.application.dumb.IndexNotReadyException;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorFactory;
import consulo.codeEditor.event.EditorFactoryEvent;
import consulo.codeEditor.event.EditorFactoryListener;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ide.impl.idea.codeInsight.hint.EditorHintListener;
import consulo.ide.impl.idea.ui.LightweightHintImpl;
import consulo.language.editor.CodeInsightSettings;
import consulo.language.editor.completion.CamelHumpMatcher;
import consulo.language.editor.completion.CompletionProcess;
import consulo.language.editor.completion.CompletionService;
import consulo.language.editor.completion.lookup.*;
import consulo.language.editor.completion.lookup.event.LookupEvent;
import consulo.language.editor.completion.lookup.event.LookupListener;
import consulo.language.editor.documentation.DocumentationManager;
import consulo.language.editor.hint.HintManager;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.event.DumbModeListener;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.dataholder.Key;
import consulo.util.lang.BitUtil;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import kava.beans.PropertyChangeListener;
import kava.beans.PropertyChangeSupport;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@Singleton
@ServiceImpl
public class LookupManagerImpl extends LookupManager {
  private static final Logger LOG = Logger.getInstance(LookupManagerImpl.class);
  private final Project myProject;
  private LookupImpl myActiveLookup = null;
  private Editor myActiveLookupEditor = null;
  private final PropertyChangeSupport myPropertyChangeSupport = new PropertyChangeSupport(this);

  public static final Key<Boolean> SUPPRESS_AUTOPOPUP_JAVADOC = Key.create("LookupManagerImpl.suppressAutopopupJavadoc");

  private Future<?> myUpdateDocFuture = CompletableFuture.completedFuture(null);

  @Inject
  public LookupManagerImpl(Project project) {
    myProject = project;

    project.getMessageBus().connect().subscribe(EditorHintListener.class, new EditorHintListener() {
      @Override
      public void hintShown(final Project project, @Nonnull final LightweightHintImpl hint, final int flags) {
        if (project == myProject) {
          Lookup lookup = getActiveLookup();
          if (lookup != null && BitUtil.isSet(flags, HintManager.HIDE_BY_LOOKUP_ITEM_CHANGE)) {
            lookup.addLookupListener(new LookupListener() {
              @Override
              public void currentItemChanged(@Nonnull LookupEvent event) {
                hint.hide();
              }

              @Override
              public void itemSelected(@Nonnull LookupEvent event) {
                hint.hide();
              }

              @Override
              public void lookupCanceled(@Nonnull LookupEvent event) {
                hint.hide();
              }
            });
          }
        }
      }
    });

    project.getMessageBus().connect().subscribe(DumbModeListener.class, new DumbModeListener() {
      @Override
      public void enteredDumbMode() {
        hideActiveLookup();
      }

      @Override
      public void exitDumbMode() {
        hideActiveLookup();
      }
    });


    EditorFactory.getInstance().addEditorFactoryListener(new EditorFactoryListener() {
      @Override
      public void editorReleased(@Nonnull EditorFactoryEvent event) {
        if (event.getEditor() == myActiveLookupEditor) {
          hideActiveLookup();
        }
      }
    }, myProject);
  }

  @Override
  public LookupEx showLookup(@Nonnull final Editor editor,
                             @Nonnull LookupElement[] items,
                             @Nonnull final String prefix,
                             @Nonnull final LookupArranger arranger) {
    for (LookupElement item : items) {
      assert item != null;
    }

    LookupImpl lookup = createLookup(editor, items, prefix, arranger);
    return lookup.showLookup() ? lookup : null;
  }

  @Nonnull
  @Override
  public LookupImpl createLookup(@Nonnull final Editor editor,
                                 @Nonnull LookupElement[] items,
                                 @Nonnull final String prefix,
                                 @Nonnull final LookupArranger arranger) {
    hideActiveLookup();

    final LookupImpl lookup = createLookup(editor, arranger, myProject);

    UIAccess.assertIsUIThread();

    myActiveLookup = lookup;
    myActiveLookupEditor = editor;
    myActiveLookup.addLookupListener(new LookupListener() {
      @Override
      public void itemSelected(@Nonnull LookupEvent event) {
        lookupClosed();
      }

      @Override
      public void lookupCanceled(@Nonnull LookupEvent event) {
        lookupClosed();
      }

      @Override
      public void currentItemChanged(@Nonnull LookupEvent event) {
        myUpdateDocFuture.cancel(false);
        CodeInsightSettings settings = CodeInsightSettings.getInstance();
        if (settings.AUTO_POPUP_JAVADOC_INFO && DocumentationManager.getInstance(myProject).getDocInfoHint() == null) {
          myUpdateDocFuture =
            myProject.getUIAccess().getScheduler().schedule(() -> showJavadoc(lookup), settings.JAVADOC_INFO_DELAY, TimeUnit.MILLISECONDS);
        }
      }

      @RequiredUIAccess
      private void lookupClosed() {
        UIAccess.assertIsUIThread();
        myUpdateDocFuture.cancel(false);
        lookup.removeLookupListener(this);
      }
    });
    Disposer.register(lookup, () -> {
      myActiveLookup = null;
      myActiveLookupEditor = null;
      myPropertyChangeSupport.firePropertyChange(PROP_ACTIVE_LOOKUP, lookup, null);
    });

    if (items.length > 0) {
      CamelHumpMatcher matcher = new CamelHumpMatcher(prefix);
      for (LookupElement item : items) {
        myActiveLookup.addItem(item, matcher);
      }
      myActiveLookup.refreshUi(true, true);
    }
    else {
      myUpdateDocFuture.cancel(false); // no items -> no doc
    }

    myPropertyChangeSupport.firePropertyChange(PROP_ACTIVE_LOOKUP, null, myActiveLookup);
    return lookup;
  }

  private void showJavadoc(LookupImpl lookup) {
    if (myActiveLookup != lookup) return;

    DocumentationManager docManager = DocumentationManager.getInstance(myProject);
    if (docManager.getDocInfoHint() != null) return; // will auto-update

    LookupElement currentItem = lookup.getCurrentItem();
    CompletionProcess completion = CompletionService.getCompletionService().getCurrentCompletion();
    if (currentItem != null && currentItem.isValid() && isAutoPopupJavadocSupportedBy(currentItem) && completion != null) {
      try {
        boolean hideLookupWithDoc = completion.isAutopopupCompletion() || CodeInsightSettings.getInstance().JAVADOC_INFO_DELAY == 0;
        docManager.showJavaDocInfo(lookup.getEditor(), lookup.getPsiFile(), false, () -> {
          if (hideLookupWithDoc && completion == CompletionService.getCompletionService().getCurrentCompletion()) {
            hideActiveLookup();
          }
        });
      }
      catch (IndexNotReadyException ignored) {
      }
    }
  }

  protected boolean isAutoPopupJavadocSupportedBy(@SuppressWarnings("unused") LookupElement lookupItem) {
    return lookupItem.getUserData(SUPPRESS_AUTOPOPUP_JAVADOC) == null;
  }

  @Nonnull
  protected LookupImpl createLookup(@Nonnull Editor editor, @Nonnull LookupArranger arranger, Project project) {
    return new LookupImpl(project, editor, arranger);
  }

  @Override
  public void hideActiveLookup() {
    LookupImpl lookup = myActiveLookup;
    if (lookup != null) {
      lookup.checkValid();
      lookup.hide();
      LOG.assertTrue(lookup.isLookupDisposed(), "Should be disposed");
    }
  }

  @Override
  public LookupEx getActiveLookup() {
    if (myActiveLookup != null && myActiveLookup.isLookupDisposed()) {
      LookupImpl lookup = myActiveLookup;
      myActiveLookup = null;
      lookup.checkValid();
    }

    return myActiveLookup;
  }

  @Override
  public void addPropertyChangeListener(@Nonnull PropertyChangeListener listener) {
    myPropertyChangeSupport.addPropertyChangeListener(listener);
  }

  @Override
  public void addPropertyChangeListener(@Nonnull final PropertyChangeListener listener, @Nonnull Disposable disposable) {
    addPropertyChangeListener(listener);
    Disposer.register(disposable, new Disposable() {
      @Override
      public void dispose() {
        removePropertyChangeListener(listener);
      }
    });
  }

  @Override
  public void removePropertyChangeListener(@Nonnull PropertyChangeListener listener) {
    myPropertyChangeSupport.removePropertyChangeListener(listener);
  }
}
