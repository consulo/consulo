/*
 * Copyright 2013-2026 consulo.io
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
package consulo.ide.impl.navigation;

import consulo.annotation.component.ServiceImpl;
import consulo.application.concurrent.coroutine.ReadLock;
import consulo.language.editor.ui.navigation.PsiTargetNavigationService;
import consulo.language.editor.ui.navigation.PsiTargetNavigator;
import consulo.language.editor.ui.navigation.PsiTargetPresentationFactory;
import consulo.language.psi.PsiElement;
import consulo.util.concurrent.coroutine.CoroutineStep;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.Collection;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2026-08-27
 */
@Singleton
@ServiceImpl
public class PsiTargetNavigationServiceImpl implements PsiTargetNavigationService {
    private final PsiTargetPresentationFactory myPresentationFactory;

    @Inject
    public PsiTargetNavigationServiceImpl(PsiTargetPresentationFactory presentationFactory) {
        myPresentationFactory = presentationFactory;
    }

    @Override
    public <T extends PsiElement> PsiTargetNavigator<T> newNavigator(Supplier<Collection<T>> targets) {
        return new PsiTargetNavigatorImpl<>(ReadLock.apply(ignored -> targets.get()), myPresentationFactory);
    }

    @Override
    public <T extends PsiElement> PsiTargetNavigator<T> newNavigator(CoroutineStep<Void, Collection<T>> targets) {
        return new PsiTargetNavigatorImpl<>(targets, myPresentationFactory);
    }
}
