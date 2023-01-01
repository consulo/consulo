// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.language.editor.completion.lookup;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.codeEditor.Editor;
import consulo.disposer.Disposable;
import consulo.language.editor.inject.EditorWindow;
import consulo.project.Project;
import kava.beans.PropertyChangeListener;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@ServiceAPI(ComponentScope.PROJECT)
public abstract class LookupManager {
  public static LookupManager getInstance(@Nonnull Project project) {
    return project.getInstance(LookupManager.class);
  }

  @Nullable
  public static LookupEx getActiveLookup(@Nullable Editor editor) {
    if (editor == null) return null;

    final Project project = editor.getProject();
    if (project == null || project.isDisposed()) return null;

    final LookupEx lookup = getInstance(project).getActiveLookup();
    if (lookup == null) return null;

    return lookup.getTopLevelEditor() == EditorWindow.getTopLevelEditor(editor) ? lookup : null;
  }

  @Nullable
  public LookupEx showLookup(@Nonnull Editor editor, @Nonnull LookupElement... items) {
    return showLookup(editor, items, "", new LookupArranger.DefaultArranger());
  }

  @Nullable
  public LookupEx showLookup(@Nonnull Editor editor, @Nonnull LookupElement[] items, @Nonnull String prefix) {
    return showLookup(editor, items, prefix, new LookupArranger.DefaultArranger());
  }

  @Nullable
  public abstract LookupEx showLookup(@Nonnull Editor editor, @Nonnull LookupElement[] items, @Nonnull String prefix, @Nonnull LookupArranger arranger);

  public abstract void hideActiveLookup();

  public static void hideActiveLookup(@Nonnull Project project) {
    LookupManager lookupManager = LookupManager.getInstance(project);
    lookupManager.hideActiveLookup();
  }

  @Nullable
  public abstract LookupEx getActiveLookup();

  @NonNls
  public static final String PROP_ACTIVE_LOOKUP = "activeLookup";

  public abstract void addPropertyChangeListener(@Nonnull PropertyChangeListener listener);

  public abstract void addPropertyChangeListener(@Nonnull PropertyChangeListener listener, @Nonnull Disposable disposable);

  public abstract void removePropertyChangeListener(@Nonnull PropertyChangeListener listener);

  @Nonnull
  public abstract Lookup createLookup(@Nonnull Editor editor, @Nonnull LookupElement[] items, @Nonnull final String prefix, @Nonnull LookupArranger arranger);

}