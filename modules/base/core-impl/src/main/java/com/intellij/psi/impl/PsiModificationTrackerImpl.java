/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.psi.impl;

import com.intellij.lang.Language;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.TransactionGuard;
import com.intellij.openapi.application.TransactionGuardImpl;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.ModificationTracker;
import com.intellij.openapi.util.SimpleModificationTracker;
import com.intellij.psi.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.util.containers.ConcurrentFactoryMap;
import com.intellij.util.messages.MessageBus;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;

import static com.intellij.psi.impl.PsiTreeChangeEventImpl.PsiEventType.CHILD_MOVED;
import static com.intellij.psi.impl.PsiTreeChangeEventImpl.PsiEventType.PROPERTY_CHANGED;

/**
 * @author mike
 */
@Singleton
public class PsiModificationTrackerImpl implements PsiModificationTracker, PsiTreeChangePreprocessor {
  private static final boolean ourEnableCodeBlockTracker = false;
  private static final boolean ourEnableJavaStructureTracker = false;
  private static final boolean ourEnableLanguageTracker = false;

  private final boolean myTestMode = false;

  private final SimpleModificationTracker myModificationCount = new SimpleModificationTracker();
  private final SimpleModificationTracker myOutOfCodeBlockModificationTracker = wrapped(ourEnableCodeBlockTracker, myModificationCount, myTestMode);
  private final SimpleModificationTracker myJavaStructureModificationTracker = wrapped(ourEnableJavaStructureTracker, myModificationCount, myTestMode);

  private final Map<Language, ModificationTracker> myLanguageTrackers = ConcurrentFactoryMap.createMap(language -> new SimpleModificationTracker());

  private final Listener myPublisher;

  @Inject
  public PsiModificationTrackerImpl(Project project) {
    MessageBus bus = project.getMessageBus();
    myPublisher = bus.syncPublisher(TOPIC);
    bus.connect().subscribe(DumbService.DUMB_MODE, new DumbService.DumbModeListener() {
      private void doIncCounter() {
        ApplicationManager.getApplication().runWriteAction(() -> incCounter());
      }

      @Override
      public void enteredDumbMode() {
        doIncCounter();
      }

      @Override
      public void exitDumbMode() {
        doIncCounter();
      }
    });
  }

  public void incCounter() {
    incCountersInner(7);
  }

  public void incOutOfCodeBlockModificationCounter() {
    incCountersInner(3);
  }

  private void fireEvent() {
    ((TransactionGuardImpl)TransactionGuard.getInstance()).assertWriteActionAllowed();
    myPublisher.modificationCountChanged();
  }

  private void incCountersInner(int bits) {
    if ((bits & 0x1) != 0) myModificationCount.incModificationCount();
    if ((bits & 0x2) != 0) myOutOfCodeBlockModificationTracker.incModificationCount();
    if ((bits & 0x4) != 0) myJavaStructureModificationTracker.incModificationCount();
    fireEvent();
  }

  @Override
  public void treeChanged(@Nonnull PsiTreeChangeEventImpl event) {
    if (!canAffectPsi(event)) {
      return;
    }

    incLanguageTrackers(event);

    PsiTreeChangeEventImpl.PsiEventType code = event.getCode();
    boolean outOfCodeBlock = code == PROPERTY_CHANGED
                             ? event.getPropertyName() == PsiTreeChangeEvent.PROP_UNLOADED_PSI || event.getPropertyName() == PsiTreeChangeEvent.PROP_ROOTS
                             : code == CHILD_MOVED ? event.getOldParent() instanceof PsiDirectory || event.getNewParent() instanceof PsiDirectory : event.getParent() instanceof PsiDirectory;

    incCountersInner(outOfCodeBlock ? 7 : 1);
  }

  public static boolean canAffectPsi(@Nonnull PsiTreeChangeEventImpl event) {
    return !PsiTreeChangeEvent.PROP_WRITABLE.equals(event.getPropertyName());
  }

  protected void incLanguageTrackers(@Nonnull PsiTreeChangeEventImpl event) {
    if (!ourEnableLanguageTracker) return;
    incLanguageModificationCount(Language.ANY);
    for (PsiElement o : new PsiElement[]{event.getFile(), event.getParent(), event.getOldParent(), event.getNewParent(), event.getElement(), event.getChild(), event.getOldChild(), event.getNewChild()}) {
      PsiFile file = o instanceof PsiFile ? (PsiFile)o : null;
      if (file == null) {
        try {
          IElementType type = PsiUtilCore.getElementType(o);
          Language language = type != null ? type.getLanguage() : o != null ? o.getLanguage() : null;
          incLanguageModificationCount(language);
        }
        catch (PsiInvalidElementAccessException e) {
          PsiDocumentManagerBase.LOG.warn(e);
        }
      }
      else {
        for (Language language : file.getViewProvider().getLanguages()) {
          incLanguageModificationCount(language);
        }
      }
    }
  }

  @Override
  public long getModificationCount() {
    return myModificationCount.getModificationCount();
  }

  @Override
  public long getOutOfCodeBlockModificationCount() {
    return myOutOfCodeBlockModificationTracker.getModificationCount();
  }

  @Override
  public long getJavaStructureModificationCount() {
    return myJavaStructureModificationTracker.getModificationCount();
  }

  @Nonnull
  @Override
  public ModificationTracker getOutOfCodeBlockModificationTracker() {
    return myOutOfCodeBlockModificationTracker;
  }

  @Nonnull
  @Override
  public ModificationTracker getJavaStructureModificationTracker() {
    return myJavaStructureModificationTracker;
  }

  public boolean isEnableCodeBlockTracker() {
    if (myTestMode) return true;
    return ourEnableCodeBlockTracker;
  }

  public boolean isEnableLanguageTracker() {
    return ourEnableLanguageTracker;
  }

  public void incLanguageModificationCount(@Nullable Language language) {
    if (language == null) return;
    ((SimpleModificationTracker)myLanguageTrackers.get(language)).incModificationCount();
  }

  @Nonnull
  public ModificationTracker forLanguage(@Nonnull Language language) {
    if (!ourEnableLanguageTracker) return this;
    return myLanguageTrackers.get(language);
  }

  @Nonnull
  public ModificationTracker forLanguages(@Nonnull Condition<? super Language> condition) {
    if (!ourEnableLanguageTracker) return this;
    return () -> {
      long result = 0;
      for (Language l : myLanguageTrackers.keySet()) {
        if (condition.value(l)) continue;
        result += myLanguageTrackers.get(l).getModificationCount();
      }
      return result;
    };
  }

  @Nonnull
  private static SimpleModificationTracker wrapped(boolean value, SimpleModificationTracker fallback, boolean testMode) {
    if (testMode) {
      return new SimpleModificationTracker();
    }
    return new SimpleModificationTracker() {
      @Override
      public long getModificationCount() {
        return value ? super.getModificationCount() : fallback.getModificationCount();
      }

      @Override
      public void incModificationCount() {
        if (value) super.incModificationCount();
        //else fallback.incModificationCount();
      }
    };
  }
}
