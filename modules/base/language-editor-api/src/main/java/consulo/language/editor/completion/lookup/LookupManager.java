// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.language.editor.completion.lookup;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.codeEditor.Editor;
import consulo.disposer.Disposable;
import consulo.language.editor.inject.EditorWindow;
import consulo.project.Project;
import kava.beans.PropertyChangeListener;

import org.jspecify.annotations.Nullable;

@ServiceAPI(ComponentScope.PROJECT)
public abstract class LookupManager {
  public static LookupManager getInstance(Project project) {
    return project.getInstance(LookupManager.class);
  }

  @Nullable
  public static LookupEx getActiveLookup(@Nullable Editor editor) {
    if (editor == null) return null;

    Project project = editor.getProject();
    if (project == null || project.isDisposed()) return null;

    LookupEx lookup = getInstance(project).getActiveLookup();
    if (lookup == null) return null;

    return lookup.getTopLevelEditor() == EditorWindow.getTopLevelEditor(editor) ? lookup : null;
  }

  @Nullable
  public LookupEx showLookup(Editor editor, LookupElement... items) {
    return showLookup(editor, items, "", new LookupArranger.DefaultArranger());
  }

  @Nullable
  public LookupEx showLookup(Editor editor, LookupElement[] items, String prefix) {
    return showLookup(editor, items, prefix, new LookupArranger.DefaultArranger());
  }

  @Nullable
  public abstract LookupEx showLookup(Editor editor, LookupElement[] items, String prefix, LookupArranger arranger);

  public abstract void hideActiveLookup();

  public static void hideActiveLookup(Project project) {
    LookupManager lookupManager = LookupManager.getInstance(project);
    lookupManager.hideActiveLookup();
  }

  @Nullable
  public abstract LookupEx getActiveLookup();

  
  public static final String PROP_ACTIVE_LOOKUP = "activeLookup";

  public abstract void addPropertyChangeListener(PropertyChangeListener listener);

  public abstract void addPropertyChangeListener(PropertyChangeListener listener, Disposable disposable);

  public abstract void removePropertyChangeListener(PropertyChangeListener listener);

  
  public abstract Lookup createLookup(Editor editor, LookupElement[] items, String prefix, LookupArranger arranger);

}