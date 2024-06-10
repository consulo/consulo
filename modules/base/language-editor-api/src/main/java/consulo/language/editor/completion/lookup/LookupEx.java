/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.language.editor.completion.lookup;

import consulo.application.util.matcher.PrefixMatcher;
import consulo.disposer.Disposable;
import consulo.ui.image.Image;
import consulo.util.lang.Pair;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.*;
import java.awt.event.InputEvent;
import java.util.List;
import java.util.Map;

/**
 * @author peter
 */
public interface LookupEx extends Lookup, Disposable {
  void setCurrentItem(LookupElement item);

  Component getComponent();

  void showElementActions(@Nullable InputEvent event);

  void hideLookup(boolean explicitly);

  void finishLookup(final char completionChar);

  void finishLookup(char completionChar, @Nullable final LookupElement item);

  void setFocusDegree(LookupFocusDegree focusDegree);

  boolean performGuardedChange(Runnable change);

  LookupFocusDegree getLookupFocusDegree();

  default void hide() {
    hideLookup(true);
  }

  void setStartCompletionWhenNothingMatches(boolean startCompletionWhenNothingMatches);

  void addAdvertisement(@Nonnull String text, @Nullable Image icon);

  int getLookupOriginalStart();

  void truncatePrefix(boolean preserveSelection, int hideOffset);

  boolean isAvailableToUser();

  void refreshUi(boolean mayCheckReused, boolean onExplicitAction);

  LookupElement getCurrentItemOrEmpty();

  void markReused();

  void setCancelOnClickOutside(final boolean b);

  void setCancelOnOtherWindowOpen(final boolean b);

  boolean isLookupDisposed();

  boolean isVisible();

  void setCalculating(boolean calculating);

  boolean isCalculating();

  void markSelectionTouched();

  void updateLookupWidth();

  void requestResize();

  void fireBeforeAppendPrefix(char c);

  void appendPrefix(char c);

  String getAdditionalPrefix();

  boolean addItem(LookupElement item, PrefixMatcher matcher);

  void setArranger(LookupArranger arranger);

  boolean isShown();

  void ensureSelectionVisible(boolean forceTopSelection);

  boolean showLookup();

  boolean mayBeNoticed();

  LookupAdvertiser getAdvertiser();

  Map<LookupElement, List<Pair<String, Object>>> getRelevanceObjects(@Nonnull Iterable<LookupElement> items, boolean hideSingleValued);

  void moveUp();

  void moveDown();

  void movePageUp();

  void movePageDown();

  void moveHome();

  void moveEnd();

  void checkValid();

  void replacePrefix(final String presentPrefix, final String newPrefix);

  void finishLookupInWritableFile(char completionChar, @Nullable LookupElement item);

  boolean isStartCompletionWhenNothingMatches();

  int getSelectedIndex();
}
