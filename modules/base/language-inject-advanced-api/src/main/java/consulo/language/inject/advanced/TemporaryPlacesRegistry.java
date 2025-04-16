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

package consulo.language.inject.advanced;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.language.Language;
import consulo.language.editor.completion.CompletionUtilCore;
import consulo.language.psi.*;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.Lists;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.Collections;
import java.util.List;

/**
 * @author Gregory.Shrago
 */
@Singleton
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
public class TemporaryPlacesRegistry {
    private final Project myProject;
    private final List<TempPlace> myTempPlaces = Lists.newLockFreeCopyOnWriteList();

    private final PsiModificationTracker myModificationTracker;
    private volatile long myPsiModificationCounter;

    private final LanguageInjectionSupport myInjectorSupport = new AbstractLanguageInjectionSupport() {
        @Nonnull
        @Override
        public String getId() {
            return "temp";
        }

        @Override
        public boolean isApplicableTo(PsiLanguageInjectionHost host) {
            return true;
        }

        @Nonnull
        @Override
        public Class[] getPatternClasses() {
            return ArrayUtil.EMPTY_CLASS_ARRAY;
        }

        @Override
        @RequiredUIAccess
        public boolean addInjectionInPlace(Language language, PsiLanguageInjectionHost host) {
            addHostWithUndo(host, InjectedLanguage.create(language.getID()));
            return true;
        }

        @Override
        @RequiredUIAccess
        public boolean removeInjectionInPlace(PsiLanguageInjectionHost psiElement) {
            return removeHostWithUndo(myProject, psiElement);
        }
    };

    public static TemporaryPlacesRegistry getInstance(Project project) {
        return project.getInstance(TemporaryPlacesRegistry.class);
    }

    @Inject
    public TemporaryPlacesRegistry(Project project, PsiModificationTracker modificationTracker) {
        myProject = project;
        myModificationTracker = modificationTracker;
    }

    private List<TempPlace> getInjectionPlacesSafe() {
        long modificationCount = myModificationTracker.getModificationCount();
        if (myPsiModificationCounter == modificationCount) {
            return myTempPlaces;
        }
        myPsiModificationCounter = modificationCount;
        List<TempPlace> placesToRemove = ContainerUtil.findAll(
            myTempPlaces,
            place -> {
                PsiLanguageInjectionHost element = place.elementPointer.getElement();
                if (element == null) {
                    return true;
                }
                else {
                    element.putUserData(LanguageInjectionSupport.TEMPORARY_INJECTED_LANGUAGE, place.language);
                    return false;
                }
            }
        );
        if (!placesToRemove.isEmpty()) {
            myTempPlaces.removeAll(placesToRemove);
        }
        return myTempPlaces;
    }

    @RequiredReadAction
    private void addInjectionPlace(TempPlace place) {
        PsiLanguageInjectionHost element = place.elementPointer.getElement();
        if (element == null) {
            return;
        }
        List<TempPlace> injectionPoints = getInjectionPlacesSafe();
        element.putUserData(LanguageInjectionSupport.TEMPORARY_INJECTED_LANGUAGE, place.language);

        for (TempPlace tempPlace : injectionPoints) {
            if (tempPlace.elementPointer.getElement() == element) {
                injectionPoints.remove(tempPlace);
                break;
            }
        }
        if (place.language != null) {
            injectionPoints.add(place);
        }
    }

    @RequiredUIAccess
    public boolean removeHostWithUndo(Project project, PsiLanguageInjectionHost host) {
        InjectedLanguage prevLanguage = host.getUserData(LanguageInjectionSupport.TEMPORARY_INJECTED_LANGUAGE);
        if (prevLanguage == null) {
            return false;
        }
        SmartPointerManager manager = SmartPointerManager.getInstance(myProject);
        SmartPsiElementPointer<PsiLanguageInjectionHost> pointer = manager.createSmartPsiElementPointer(host);
        TempPlace place = new TempPlace(prevLanguage, pointer);
        TempPlace nextPlace = new TempPlace(null, pointer);
        Configuration.replaceInjectionsWithUndo(
            project,
            nextPlace,
            place,
            Collections.<PsiElement>emptyList(),
            (add, remove) -> {
                addInjectionPlace(add);
                return true;
            }
        );
        return true;
    }

    @RequiredUIAccess
    public void addHostWithUndo(PsiLanguageInjectionHost host, InjectedLanguage language) {
        InjectedLanguage prevLanguage = host.getUserData(LanguageInjectionSupport.TEMPORARY_INJECTED_LANGUAGE);
        SmartPointerManager manager = SmartPointerManager.getInstance(myProject);
        SmartPsiElementPointer<PsiLanguageInjectionHost> pointer = manager.createSmartPsiElementPointer(host);
        TempPlace prevPlace = new TempPlace(prevLanguage, pointer);
        TempPlace place = new TempPlace(language, pointer);
        Configuration.replaceInjectionsWithUndo(
            myProject,
            place,
            prevPlace,
            Collections.<PsiElement>emptyList(),
            (add, remove) -> {
                addInjectionPlace(add);
                return true;
            }
        );
    }

    public LanguageInjectionSupport getLanguageInjectionSupport() {
        return myInjectorSupport;
    }

    @Nullable
    @RequiredReadAction
    public InjectedLanguage getLanguageFor(@Nonnull PsiLanguageInjectionHost host, PsiFile containingFile) {
        PsiLanguageInjectionHost originalHost = CompletionUtilCore.getOriginalElement(host, containingFile);
        PsiLanguageInjectionHost injectionHost = originalHost == null ? host : originalHost;
        getInjectionPlacesSafe();
        return injectionHost.getUserData(LanguageInjectionSupport.TEMPORARY_INJECTED_LANGUAGE);
    }

    private static class TempPlace {
        public final InjectedLanguage language;
        public final SmartPsiElementPointer<PsiLanguageInjectionHost> elementPointer;

        public TempPlace(InjectedLanguage language, SmartPsiElementPointer<PsiLanguageInjectionHost> elementPointer) {
            this.language = language;
            this.elementPointer = elementPointer;
        }
    }
}
