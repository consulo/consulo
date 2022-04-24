// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.inject.impl.internal;

import consulo.component.util.SimpleModificationTracker;
import consulo.language.impl.internal.psi.PsiManagerEx;
import consulo.language.impl.internal.psi.PsiParameterizedCachedValue;
import consulo.language.inject.ConcatenationAwareInjector;
import consulo.language.inject.MultiHostInjector;
import consulo.language.inject.MultiHostRegistrar;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.application.util.CachedValueProvider;
import consulo.application.util.CachedValuesManager;
import consulo.application.util.ParameterizedCachedValue;
import consulo.project.Project;
import consulo.util.collection.Lists;
import consulo.util.dataholder.Key;
import consulo.util.lang.Pair;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import java.util.List;

@Singleton
public final class ConcatenationInjectorManager extends SimpleModificationTracker {
  @Nonnull
  public static ConcatenationInjectorManager getInstance(@Nonnull Project project) {
    return project.getComponent(ConcatenationInjectorManager.class);
  }

  private final List<ConcatenationAwareInjector> myConcatenationInjectors = Lists.newLockFreeCopyOnWriteList();

  @Inject
  public ConcatenationInjectorManager(@Nonnull Project project) {
    List<ConcatenationAwareInjector> extensionList = ConcatenationAwareInjector.EP_NAME.getExtensionList(project);

    for (ConcatenationAwareInjector concatenationAwareInjector : extensionList) {
      registerConcatenationInjector(concatenationAwareInjector);
    }

    // clear caches even on non-physical changes
    PsiManagerEx.getInstanceEx(project).registerRunnableToRunOnAnyChange(this::incModificationCount);
  }

  private static InjectionResult doCompute(@Nonnull PsiFile containingFile, @Nonnull Project project, @Nonnull PsiElement anchor, @Nonnull PsiElement[] operands) {
    PsiDocumentManager docManager = PsiDocumentManager.getInstance(project);
    InjectionRegistrarImpl registrar = new InjectionRegistrarImpl(project, containingFile, anchor, docManager);
    InjectionResult result = null;
    ConcatenationInjectorManager concatenationInjectorManager = getInstance(project);
    for (ConcatenationAwareInjector concatenationInjector : concatenationInjectorManager.myConcatenationInjectors) {
      concatenationInjector.inject(registrar, operands);
      result = registrar.getInjectedResult();
      if (result != null) break;
    }

    return result;
  }

  private static final Key<ParameterizedCachedValue<InjectionResult, PsiElement>> INJECTED_PSI_IN_CONCATENATION = Key.create("INJECTED_PSI_IN_CONCATENATION");
  private static final Key<Integer> NO_CONCAT_INJECTION_TIMESTAMP = Key.create("NO_CONCAT_INJECTION_TIMESTAMP");

  public abstract static class BaseConcatenation2InjectorAdapter implements MultiHostInjector {
    private final Project myProject;

    public BaseConcatenation2InjectorAdapter(@Nonnull Project project) {
      myProject = project;
    }

    @Override
    public void injectLanguages(@Nonnull MultiHostRegistrar registrar, @Nonnull PsiElement context) {
      ConcatenationInjectorManager manager = getInstance(myProject);
      if (manager.myConcatenationInjectors.isEmpty()) {
        return;
      }

      final PsiFile containingFile = ((InjectionRegistrarImpl)registrar).getHostPsiFile();
      Project project = containingFile.getProject();
      long modificationCount = PsiManager.getInstance(project).getModificationTracker().getModificationCount();
      Pair<PsiElement, PsiElement[]> pair = computeAnchorAndOperands(context);
      PsiElement anchor = pair.first;
      PsiElement[] operands = pair.second;
      Integer noInjectionTimestamp = anchor.getUserData(NO_CONCAT_INJECTION_TIMESTAMP);

      InjectionResult result;
      ParameterizedCachedValue<InjectionResult, PsiElement> data = null;
      if (operands.length == 0 || noInjectionTimestamp != null && noInjectionTimestamp == modificationCount) {
        result = null;
      }
      else {
        data = anchor.getUserData(INJECTED_PSI_IN_CONCATENATION);

        result = data == null ? null : data.getValue(context);
        if (result == null || !result.isValid()) {
          result = doCompute(containingFile, project, anchor, operands);
        }
      }
      if (result != null) {
        ((InjectionRegistrarImpl)registrar).addToResults(result);

        if (data == null) {
          CachedValueProvider.Result<InjectionResult> cachedResult = CachedValueProvider.Result.create(result, manager);
          data = CachedValuesManager.getManager(project).createParameterizedCachedValue(context1 -> {
            PsiFile containingFile1 = context1.getContainingFile();
            Project project1 = containingFile1.getProject();
            Pair<PsiElement, PsiElement[]> pair1 = computeAnchorAndOperands(context1);
            InjectionResult result1 = pair1.second.length == 0 ? null : doCompute(containingFile1, project1, pair1.first, pair1.second);
            return result1 == null ? null : CachedValueProvider.Result.create(result1, manager);
          }, false);
          ((PsiParameterizedCachedValue<InjectionResult, PsiElement>)data).setValue(cachedResult);

          anchor.putUserData(INJECTED_PSI_IN_CONCATENATION, data);
          if (anchor.getUserData(NO_CONCAT_INJECTION_TIMESTAMP) != null) {
            anchor.putUserData(NO_CONCAT_INJECTION_TIMESTAMP, null);
          }
        }
      }
      else {
        // cache no-injection flag
        if (anchor.getUserData(INJECTED_PSI_IN_CONCATENATION) != null) {
          anchor.putUserData(INJECTED_PSI_IN_CONCATENATION, null);
        }
        anchor.putUserData(NO_CONCAT_INJECTION_TIMESTAMP, (int)modificationCount);
      }
    }

    protected abstract Pair<PsiElement, PsiElement[]> computeAnchorAndOperands(@Nonnull PsiElement context);
  }


  private void registerConcatenationInjector(@Nonnull ConcatenationAwareInjector injector) {
    myConcatenationInjectors.add(injector);
    concatenationInjectorsChanged();
  }

  private boolean unregisterConcatenationInjector(@Nonnull ConcatenationAwareInjector injector) {
    boolean removed = myConcatenationInjectors.remove(injector);
    concatenationInjectorsChanged();
    return removed;
  }

  private void concatenationInjectorsChanged() {
    incModificationCount();
  }
}
