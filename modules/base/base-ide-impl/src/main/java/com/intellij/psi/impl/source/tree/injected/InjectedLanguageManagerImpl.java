// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.psi.impl.source.tree.injected;

import com.intellij.injected.editor.DocumentWindow;
import com.intellij.injected.editor.VirtualFileWindow;
import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.lang.injection.MultiHostInjector;
import com.intellij.lang.injection.MultiHostRegistrar;
import consulo.disposer.Disposable;
import consulo.logging.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.ex.DocumentEx;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.Segment;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.impl.PsiDocumentManagerBase;
import com.intellij.psi.impl.source.resolve.FileContextUtil;
import com.intellij.util.ArrayUtil;
import com.intellij.util.Processor;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.MultiMap;
import consulo.lang.injection.MultiHostInjectorExtensionPoint;
import jakarta.inject.Inject;
import org.jetbrains.annotations.TestOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import jakarta.inject.Singleton;
import java.util.*;

/**
 * @author cdr
 */
@SuppressWarnings("deprecation")
@Singleton
public class InjectedLanguageManagerImpl extends InjectedLanguageManager implements Disposable {
  private static final Logger LOG = Logger.getInstance(InjectedLanguageManagerImpl.class);
  @SuppressWarnings("RedundantStringConstructorCall")
  static final Object ourInjectionPsiLock = new String("injectionPsiLock");
  private final Project myProject;
  private final DumbService myDumbService;
  private final PsiDocumentManager myDocManager;

  public static InjectedLanguageManagerImpl getInstanceImpl(Project project) {
    return (InjectedLanguageManagerImpl)InjectedLanguageManager.getInstance(project);
  }

  @Inject
  public InjectedLanguageManagerImpl(Project project, DumbService dumbService, PsiDocumentManager psiDocumentManager) {
    myProject = project;
    myDumbService = dumbService;
    myDocManager = psiDocumentManager;
  }

  @Override
  public void dispose() {
    disposeInvalidEditors();
  }

  public static void disposeInvalidEditors() {
    EditorWindowImpl.disposeInvalidEditors();
  }

  @Override
  public PsiLanguageInjectionHost getInjectionHost(@Nonnull FileViewProvider injectedProvider) {
    if (!(injectedProvider instanceof InjectedFileViewProvider)) return null;
    return ((InjectedFileViewProvider)injectedProvider).getShreds().getHostPointer().getElement();
  }

  @Override
  public PsiLanguageInjectionHost getInjectionHost(@Nonnull PsiElement injectedElement) {
    final PsiFile file = injectedElement.getContainingFile();
    final VirtualFile virtualFile = file == null ? null : file.getVirtualFile();
    if (virtualFile instanceof VirtualFileWindow) {
      PsiElement host = FileContextUtil.getFileContext(file); // use utility method in case the file's overridden getContext()
      if (host instanceof PsiLanguageInjectionHost) {
        return (PsiLanguageInjectionHost)host;
      }
    }
    return null;
  }

  @Override
  @Nonnull
  public TextRange injectedToHost(@Nonnull PsiElement injectedContext, @Nonnull TextRange injectedTextRange) {
    DocumentWindow documentWindow = getDocumentWindow(injectedContext);
    return documentWindow == null ? injectedTextRange : documentWindow.injectedToHost(injectedTextRange);
  }

  @Override
  public int injectedToHost(@Nonnull PsiElement element, int offset) {
    DocumentWindow documentWindow = getDocumentWindow(element);
    return documentWindow == null ? offset : documentWindow.injectedToHost(offset);
  }

  @Override
  public int injectedToHost(@Nonnull PsiElement injectedContext, int injectedOffset, boolean minHostOffset) {
    DocumentWindow documentWindow = getDocumentWindow(injectedContext);
    return documentWindow == null ? injectedOffset : documentWindow.injectedToHost(injectedOffset, minHostOffset);
  }

  private static DocumentWindow getDocumentWindow(@Nonnull PsiElement element) {
    PsiFile file = element.getContainingFile();
    if (file == null) return null;
    Document document = PsiDocumentManager.getInstance(file.getProject()).getCachedDocument(file);
    return !(document instanceof DocumentWindow) ? null : (DocumentWindow)document;
  }

  // used only from tests => no need for complex synchronization
  private volatile ClassMapCachingNulls<MultiHostInjector> cachedInjectors;

  public void processInjectableElements(@Nonnull Collection<? extends PsiElement> in, @Nonnull Processor<? super PsiElement> processor) {
    ClassMapCachingNulls<MultiHostInjector> map = getInjectorMap();
    for (PsiElement element : in) {
      if (map.get(element.getClass()) != null) {
        processor.process(element);
      }
    }
  }

  @Nonnull
  private ClassMapCachingNulls<MultiHostInjector> getInjectorMap() {
    ClassMapCachingNulls<MultiHostInjector> cached = cachedInjectors;
    if (cached != null) {
      return cached;
    }

    ClassMapCachingNulls<MultiHostInjector> result = calcInjectorMap();
    cachedInjectors = result;
    return result;
  }

  @Nonnull
  private ClassMapCachingNulls<MultiHostInjector> calcInjectorMap() {
    Map<Class<?>, MultiHostInjector[]> injectors = new HashMap<>();

    MultiMap<Class<? extends PsiElement>, MultiHostInjector> allInjectors = new MultiMap<>();
    for (MultiHostInjectorExtensionPoint extensionPoint : MultiHostInjector.EP_NAME.getExtensionList(myProject)) {
      allInjectors.putValue(extensionPoint.getKey(), extensionPoint.getInstance(myProject));
    }
    if (LanguageInjector.EXTENSION_POINT_NAME.hasAnyExtensions()) {
      allInjectors.putValue(PsiLanguageInjectionHost.class, PsiManagerRegisteredInjectorsAdapter.INSTANCE);
    }

    for (Map.Entry<Class<? extends PsiElement>, Collection<MultiHostInjector>> injector : allInjectors.entrySet()) {
      Class<? extends PsiElement> place = injector.getKey();
      Collection<MultiHostInjector> value = injector.getValue();
      injectors.put(place, value.toArray(new MultiHostInjector[0]));
    }

    return new ClassMapCachingNulls<>(injectors, new MultiHostInjector[0], new ArrayList<>(allInjectors.values()));
  }

  private void clearInjectorCache() {
    cachedInjectors = null;
  }

  @Nonnull
  @Override
  public String getUnescapedText(@Nonnull final PsiElement injectedNode) {
    final String leafText = InjectedLanguageUtil.getUnescapedLeafText(injectedNode, false);
    if (leafText != null) {
      return leafText; // optimization
    }
    final StringBuilder text = new StringBuilder(injectedNode.getTextLength());
    // gather text from (patched) leaves
    injectedNode.accept(new PsiRecursiveElementWalkingVisitor() {
      @Override
      public void visitElement(PsiElement element) {
        String leafText = InjectedLanguageUtil.getUnescapedLeafText(element, false);
        if (leafText != null) {
          text.append(leafText);
          return;
        }
        super.visitElement(element);
      }
    });
    return text.toString();
  }

  /**
   * intersection may spread over several injected fragments
   *
   * @param rangeToEdit range in encoded(raw) PSI
   * @return list of ranges in encoded (raw) PSI
   */
  @SuppressWarnings("ConstantConditions")
  @Override
  @Nonnull
  public List<TextRange> intersectWithAllEditableFragments(@Nonnull PsiFile injectedPsi, @Nonnull TextRange rangeToEdit) {
    Place shreds = InjectedLanguageUtil.getShreds(injectedPsi);
    if (shreds == null) return Collections.emptyList();
    Object result = null; // optimization: TextRange or ArrayList
    int count = 0;
    int offset = 0;
    for (PsiLanguageInjectionHost.Shred shred : shreds) {
      TextRange encodedRange = TextRange.from(offset + shred.getPrefix().length(), shred.getRangeInsideHost().getLength());
      TextRange intersection = encodedRange.intersection(rangeToEdit);
      if (intersection != null) {
        count++;
        if (count == 1) {
          result = intersection;
        }
        else if (count == 2) {
          TextRange range = (TextRange)result;
          if (range.isEmpty()) {
            result = intersection;
            count = 1;
          }
          else if (intersection.isEmpty()) {
            count = 1;
          }
          else {
            List<TextRange> list = new ArrayList<>();
            list.add(range);
            list.add(intersection);
            result = list;
          }
        }
        else if (intersection.isEmpty()) {
          count--;
        }
        else {
          //noinspection unchecked
          ((List<TextRange>)result).add(intersection);
        }
      }
      offset += shred.getPrefix().length() + shred.getRangeInsideHost().getLength() + shred.getSuffix().length();
    }
    //noinspection unchecked
    return count == 0 ? Collections.emptyList() : count == 1 ? Collections.singletonList((TextRange)result) : (List<TextRange>)result;
  }

  @Override
  public boolean isInjectedFragment(@Nonnull final PsiFile injectedFile) {
    return injectedFile.getViewProvider() instanceof InjectedFileViewProvider;
  }

  @Override
  public PsiElement findInjectedElementAt(@Nonnull PsiFile hostFile, int hostDocumentOffset) {
    return InjectedLanguageUtil.findInjectedElementNoCommit(hostFile, hostDocumentOffset);
  }

  @Override
  public void dropFileCaches(@Nonnull PsiFile file) {
    InjectedLanguageUtil.clearCachedInjectedFragmentsForFile(file);
  }

  @Override
  public PsiFile getTopLevelFile(@Nonnull PsiElement element) {
    return InjectedLanguageUtil.getTopLevelFile(element);
  }

  @Nonnull
  @Override
  public List<DocumentWindow> getCachedInjectedDocumentsInRange(@Nonnull PsiFile hostPsiFile, @Nonnull TextRange range) {
    return InjectedLanguageUtil.getCachedInjectedDocumentsInRange(hostPsiFile, range);
  }

  @Override
  public void enumerate(@Nonnull PsiElement host, @Nonnull PsiLanguageInjectionHost.InjectedPsiVisitor visitor) {
    InjectedLanguageUtil.enumerate(host, visitor);
  }

  @Override
  public void enumerateEx(@Nonnull PsiElement host, @Nonnull PsiFile containingFile, boolean probeUp, @Nonnull PsiLanguageInjectionHost.InjectedPsiVisitor visitor) {
    InjectedLanguageUtil.enumerate(host, containingFile, probeUp, visitor);
  }

  @Nonnull
  @Override
  public List<TextRange> getNonEditableFragments(@Nonnull DocumentWindow window) {
    List<TextRange> result = new ArrayList<>();
    int offset = 0;
    for (PsiLanguageInjectionHost.Shred shred : ((DocumentWindowImpl)window).getShreds()) {
      Segment hostRange = shred.getHostRangeMarker();
      if (hostRange == null) continue;

      offset = appendRange(result, offset, shred.getPrefix().length());
      offset += hostRange.getEndOffset() - hostRange.getStartOffset();
      offset = appendRange(result, offset, shred.getSuffix().length());
    }

    return result;
  }

  @Override
  public boolean mightHaveInjectedFragmentAtOffset(@Nonnull Document hostDocument, int hostOffset) {
    return InjectedLanguageUtil.mightHaveInjectedFragmentAtCaret(myProject, hostDocument, hostOffset);
  }

  @Nonnull
  @Override
  public DocumentWindow freezeWindow(@Nonnull DocumentWindow document) {
    Place shreds = ((DocumentWindowImpl)document).getShreds();
    Project project = shreds.getHostPointer().getProject();
    DocumentEx delegate = ((PsiDocumentManagerBase)PsiDocumentManager.getInstance(project)).getLastCommittedDocument(document.getDelegate());
    Place place = new Place();
    place.addAll(ContainerUtil.map(shreds, shred -> ((ShredImpl)shred).withPsiRange()));
    return new DocumentWindowImpl(delegate, place);
  }

  private static int appendRange(@Nonnull List<TextRange> result, int start, int length) {
    if (length > 0) {
      int lastIndex = result.size() - 1;
      TextRange lastRange = lastIndex >= 0 ? result.get(lastIndex) : null;
      if (lastRange != null && lastRange.getEndOffset() == start) {
        result.set(lastIndex, lastRange.grown(length));
      }
      else {
        result.add(TextRange.from(start, length));
      }
    }
    return start + length;
  }

  private final Map<Class<?>, MultiHostInjector[]> myInjectorsClone = new HashMap<>();

  @TestOnly
  public static void pushInjectors(@Nonnull Project project) {
    InjectedLanguageManagerImpl cachedManager = (InjectedLanguageManagerImpl)project.getUserData(INSTANCE_CACHE);
    if (cachedManager == null) return;
    try {
      assert cachedManager.myInjectorsClone.isEmpty() : cachedManager.myInjectorsClone;
    }
    finally {
      cachedManager.myInjectorsClone.clear();
    }
    cachedManager.myInjectorsClone.putAll(cachedManager.getInjectorMap().getBackingMap());
  }

  @TestOnly
  public static void checkInjectorsAreDisposed(@Nullable Project project) {
    InjectedLanguageManagerImpl cachedManager = project == null ? null : (InjectedLanguageManagerImpl)project.getUserData(INSTANCE_CACHE);
    if (cachedManager == null) return;

    try {
      ClassMapCachingNulls<MultiHostInjector> cached = cachedManager.cachedInjectors;
      if (cached == null) return;
      for (Map.Entry<Class<?>, MultiHostInjector[]> entry : cached.getBackingMap().entrySet()) {
        Class<?> key = entry.getKey();
        if (cachedManager.myInjectorsClone.isEmpty()) return;
        MultiHostInjector[] oldInjectors = cachedManager.myInjectorsClone.get(key);
        for (MultiHostInjector injector : entry.getValue()) {
          if (ArrayUtil.indexOf(oldInjectors, injector) == -1) {
            throw new AssertionError("Injector was not disposed: " + key + " -> " + injector);
          }
        }
      }
    }
    finally {
      cachedManager.myInjectorsClone.clear();
    }
  }

  InjectionResult processInPlaceInjectorsFor(@Nonnull PsiFile hostPsiFile, @Nonnull PsiElement element) {
    MultiHostInjector[] infos = getInjectorMap().get(element.getClass());
    if (infos == null || infos.length == 0) {
      return null;
    }
    final boolean dumb = myDumbService.isDumb();
    InjectionRegistrarImpl hostRegistrar = new InjectionRegistrarImpl(myProject, hostPsiFile, element, myDocManager);
    for (MultiHostInjector injector : infos) {
      if (dumb && !DumbService.isDumbAware(injector)) {
        continue;
      }

      injector.injectLanguages(hostRegistrar, element);
      InjectionResult result = hostRegistrar.getInjectedResult();
      if (result != null) return result;
    }
    return null;
  }

  @Override
  @Nullable
  public List<Pair<PsiElement, TextRange>> getInjectedPsiFiles(@Nonnull final PsiElement host) {
    if (!(host instanceof PsiLanguageInjectionHost) || !((PsiLanguageInjectionHost)host).isValidHost()) {
      return null;
    }
    final PsiElement inTree = InjectedLanguageUtil.loadTree(host, host.getContainingFile());
    final List<Pair<PsiElement, TextRange>> result = new SmartList<>();
    enumerate(inTree, (injectedPsi, places) -> {
      for (PsiLanguageInjectionHost.Shred place : places) {
        if (place.getHost() == inTree) {
          result.add(new Pair<>(injectedPsi, place.getRangeInsideHost()));
        }
      }
    });
    return result.isEmpty() ? null : result;
  }

  private static class PsiManagerRegisteredInjectorsAdapter implements MultiHostInjector {
    public static final PsiManagerRegisteredInjectorsAdapter INSTANCE = new PsiManagerRegisteredInjectorsAdapter();

    @Override
    public void injectLanguages(@Nonnull final MultiHostRegistrar injectionPlacesRegistrar, @Nonnull PsiElement context) {
      final PsiLanguageInjectionHost host = (PsiLanguageInjectionHost)context;
      InjectedLanguagePlaces placesRegistrar =
              (language, rangeInsideHost, prefix, suffix) -> injectionPlacesRegistrar.startInjecting(language).addPlace(prefix, suffix, host, rangeInsideHost).doneInjecting();
      for (LanguageInjector injector : LanguageInjector.EXTENSION_POINT_NAME.getExtensionList()) {
        injector.getLanguagesToInject(host, placesRegistrar);
      }
    }
  }
}
