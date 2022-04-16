// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.impl.internal.psi;

import consulo.annotation.access.RequiredWriteAction;
import consulo.application.Application;
import consulo.application.TransactionGuard;
import consulo.application.internal.TransactionGuardEx;
import consulo.application.util.ConcurrentFactoryMap;
import consulo.component.messagebus.MessageBus;
import consulo.component.util.ModificationTracker;
import consulo.component.util.SimpleModificationTracker;
import consulo.language.Language;
import consulo.language.ast.IElementType;
import consulo.language.psi.*;
import consulo.language.psi.event.PsiTreeChangeEvent;
import consulo.language.psi.event.PsiTreeChangePreprocessor;
import consulo.logging.Logger;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.util.lang.function.Condition;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

/**
 * @author mike
 */
@Singleton
public class PsiModificationTrackerImpl implements PsiModificationTracker, PsiTreeChangePreprocessor {
  private static final Logger LOG = Logger.getInstance(PsiModificationTrackerImpl.class);

  private final SimpleModificationTracker myModificationCount = new SimpleModificationTracker();

  private final SimpleModificationTracker myAllLanguagesTracker = new SimpleModificationTracker();
  private final Map<Language, SimpleModificationTracker> myLanguageTrackers = ConcurrentFactoryMap.createWeakMap(language -> new SimpleModificationTracker());

  private final Listener myPublisher;

  @Inject
  public PsiModificationTrackerImpl(@Nonnull Application application, @Nonnull Project project) {
    MessageBus bus = project.getMessageBus();
    myPublisher = bus.syncPublisher(TOPIC);
    bus.connect().subscribe(DumbService.DUMB_MODE, new DumbService.DumbModeListener() {
      private void doIncCounter() {
        application.runWriteAction(() -> incCounter());
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

  @Override
  @RequiredWriteAction
  public void incCounter() {
    incCountersInner();
  }

  public void incOutOfCodeBlockModificationCounter() {
    incCountersInner();
  }

  private void fireEvent() {
    ((TransactionGuardEx)TransactionGuard.getInstance()).assertWriteActionAllowed();
    myPublisher.modificationCountChanged();
  }

  private void incCountersInner() {
    myModificationCount.incModificationCount();
    fireEvent();
  }

  @Override
  public void treeChanged(@Nonnull PsiTreeChangeEvent event) {
    if (!canAffectPsi((PsiTreeChangeEventImpl)event)) {
      return;
    }

    incLanguageCounters((PsiTreeChangeEventImpl)event);
    incCountersInner();
  }

  public static boolean canAffectPsi(@Nonnull PsiTreeChangeEventImpl event) {
    PsiTreeChangeEventImpl.PsiEventType code = event.getCode();
    return !(code == PsiTreeChangeEventImpl.PsiEventType.BEFORE_PROPERTY_CHANGE || code == PsiTreeChangeEventImpl.PsiEventType.PROPERTY_CHANGED && event.getPropertyName() == PsiTreeChangeEvent.PROP_WRITABLE);
  }

  private void incLanguageCounters(@Nonnull PsiTreeChangeEventImpl event) {
    PsiTreeChangeEventImpl.PsiEventType code = event.getCode();
    String propertyName = event.getPropertyName();

    if (code == PsiTreeChangeEventImpl.PsiEventType.PROPERTY_CHANGED && (propertyName == PsiTreeChangeEvent.PROP_UNLOADED_PSI || propertyName == PsiTreeChangeEvent.PROP_ROOTS || propertyName == PsiTreeChangeEvent.PROP_FILE_TYPES) ||
        code == PsiTreeChangeEventImpl.PsiEventType.CHILD_REMOVED && event.getChild() instanceof PsiDirectory) {
      myAllLanguagesTracker.incModificationCount();
      return;
    }
    PsiElement[] elements = {event.getFile(), event.getParent(), event.getOldParent(), event.getNewParent(), event.getElement(), event.getChild(), event.getOldChild(), event.getNewChild()};
    incLanguageModificationCount(Language.ANY);
    for (PsiElement o : elements) {
      if (o == null) continue;
      if (o instanceof PsiDirectory) continue;
      if (o instanceof PsiFile) {
        for (Language language : ((PsiFile)o).getViewProvider().getLanguages()) {
          incLanguageModificationCount(language);
        }
      }
      else {
        try {
          IElementType type = PsiUtilCore.getElementType(o);
          Language language = type != null ? type.getLanguage() : o.getLanguage();
          incLanguageModificationCount(language);
        }
        catch (PsiInvalidElementAccessException e) {
          LOG.warn(e);
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
    return myModificationCount.getModificationCount();
  }

  @Override
  public long getJavaStructureModificationCount() {
    return myModificationCount.getModificationCount();
  }

  @Nonnull
  @Override
  public ModificationTracker getOutOfCodeBlockModificationTracker() {
    return myModificationCount;
  }

  @Nonnull
  @Override
  public ModificationTracker getJavaStructureModificationTracker() {
    return myModificationCount;
  }

  //@ApiStatus.Experimental
  public void incLanguageModificationCount(@Nullable Language language) {
    if (language == null) return;
    myLanguageTrackers.get(language).incModificationCount();
  }

  //@ApiStatus.Experimental
  @Nonnull
  public ModificationTracker forLanguage(@Nonnull Language language) {
    SimpleModificationTracker languageTracker = myLanguageTrackers.get(language);
    return () -> languageTracker.getModificationCount() + myAllLanguagesTracker.getModificationCount();
  }

  //@ApiStatus.Experimental
  @Nonnull
  public ModificationTracker forLanguages(@Nonnull Condition<? super Language> condition) {
    return () -> {
      long result = myAllLanguagesTracker.getModificationCount();
      for (Language l : myLanguageTrackers.keySet()) {
        if (!condition.value(l)) continue;
        result += myLanguageTrackers.get(l).getModificationCount();
      }
      return result;
    };
  }
}
