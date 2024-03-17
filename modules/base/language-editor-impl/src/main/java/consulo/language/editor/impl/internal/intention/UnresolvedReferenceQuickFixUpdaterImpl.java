/*
 * Copyright 2013-2024 consulo.io
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
package consulo.language.editor.impl.internal.intention;

import consulo.annotation.component.ServiceImpl;
import consulo.language.editor.intention.QuickFixActionRegistrar;
import consulo.language.editor.intention.UnresolvedReferenceQuickFixProvider;
import consulo.language.editor.intention.UnresolvedReferenceQuickFixUpdater;
import consulo.language.editor.rawHighlight.HighlightInfo;
import consulo.language.psi.PsiReference;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.util.lang.reflect.ReflectionUtil;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * This is simple implementation of UnresolvedReferenceQuickFixUpdater which is not do work like described in javadoc, but do like old one UnresolvedReferenceQuickFixProvider
 *
 * @author VISTALL
 * @since 2024-03-17
 */
@Singleton
@ServiceImpl
public class UnresolvedReferenceQuickFixUpdaterImpl implements UnresolvedReferenceQuickFixUpdater {
  private final Project myProject;
  private final DumbService myDumbService;

  @Inject
  public UnresolvedReferenceQuickFixUpdaterImpl(Project project, DumbService dumbService) {
    myProject = project;
    myDumbService = dumbService;
  }

  @Override
  @SuppressWarnings("unchecked")
  public void registerQuickFixesLater(@Nonnull PsiReference ref, @Nonnull HighlightInfo.Builder info) {
    boolean dumb = myDumbService.isDumb();
    Class<? extends PsiReference> referenceClass = ref.getClass();
    QuickFixActionRegistrar registrar = QuickFixActionRegistrar.create(info);

    myProject.getExtensionPoint(UnresolvedReferenceQuickFixProvider.class).forEachExtensionSafe(provider -> {
      if (dumb && !DumbService.isDumbAware(provider)) {
        return;
      }

      if (ReflectionUtil.isAssignable(provider.getReferenceClass(), referenceClass)) {
        provider.registerFixes(ref, registrar);
      }
    });
  }
}
