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

package com.intellij.psi.impl.source.tree.injected;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.codeInsight.daemon.impl.DaemonProgressIndicator;
import com.intellij.concurrency.JobLauncher;
import com.intellij.injected.editor.DocumentWindow;
import com.intellij.injected.editor.DocumentWindowImpl;
import com.intellij.injected.editor.EditorWindowImpl;
import com.intellij.injected.editor.VirtualFileWindow;
import com.intellij.lang.Language;
import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.lang.injection.MultiHostInjector;
import com.intellij.lang.injection.MultiHostRegistrar;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ex.ApplicationManagerEx;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.extensions.ExtensionPoint;
import com.intellij.openapi.extensions.ExtensionPointListener;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.extensions.PluginDescriptor;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.ProperTextRange;
import com.intellij.openapi.util.Segment;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.impl.PsiDocumentManagerBase;
import com.intellij.psi.impl.source.resolve.FileContextUtil;
import com.intellij.util.ArrayUtil;
import com.intellij.util.Processor;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ConcurrentList;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.MultiMap;
import consulo.lang.injection.MultiHostInjectorExtensionPoint;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.TestOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author cdr
 */
@Singleton
public class InjectedLanguageManagerImpl extends InjectedLanguageManager implements Disposable {
  private static final Logger LOG = Logger.getInstance(InjectedLanguageManagerImpl.class);
  @SuppressWarnings("RedundantStringConstructorCall")
  static final Object ourInjectionPsiLock = new String("injectionPsiLock");

  private final Project myProject;
  private final DumbService myDumbService;
  private final AtomicReference<MultiHostInjector> myPsiManagerRegisteredInjectorsAdapter = new AtomicReference<MultiHostInjector>();
  private volatile DaemonProgressIndicator myProgress;

  public static InjectedLanguageManagerImpl getInstanceImpl(Project project) {
    return (InjectedLanguageManagerImpl)InjectedLanguageManager.getInstance(project);
  }

  @Inject
  public InjectedLanguageManagerImpl(final Project project, DumbService dumbService) {
    myProject = project;
    myDumbService = dumbService;

    final ExtensionPoint<MultiHostInjectorExtensionPoint> multiPoint = project.getExtensionsArea().getExtensionPoint(MultiHostInjector.EP_NAME);
    multiPoint.addExtensionPointListener(new ExtensionPointListener<MultiHostInjectorExtensionPoint>() {
      @Override
      public void extensionAdded(@Nonnull MultiHostInjectorExtensionPoint ep, @Nullable PluginDescriptor pluginDescriptor) {
        registerMultiHostInjector(ep.getInstance(project), ep.getKey());
      }

      @Override
      public void extensionRemoved(@Nonnull MultiHostInjectorExtensionPoint ep, @Nullable PluginDescriptor pluginDescriptor) {
        unregisterMultiHostInjector(ep.getInstance(project));
      }
    }, this);
    final ExtensionPointListener<LanguageInjector> myListener = new ExtensionPointListener<LanguageInjector>() {
      @Override
      public void extensionAdded(@Nonnull LanguageInjector extension, @Nullable PluginDescriptor pluginDescriptor) {
        psiManagerInjectorsChanged();
      }

      @Override
      public void extensionRemoved(@Nonnull LanguageInjector extension, @Nullable PluginDescriptor pluginDescriptor) {
        psiManagerInjectorsChanged();
      }
    };
    final ExtensionPoint<LanguageInjector> psiManagerPoint = Extensions.getRootArea().getExtensionPoint(LanguageInjector.EXTENSION_POINT_NAME);
    psiManagerPoint.addExtensionPointListener(myListener, this);
    myProgress = new DaemonProgressIndicator();
    project.getMessageBus().connect(this).subscribe(DaemonCodeAnalyzer.DAEMON_EVENT_TOPIC, new DaemonCodeAnalyzer.DaemonListenerAdapter() {
      @Override
      public void daemonCancelEventOccurred(@Nonnull String reason) {
        myProgress.cancel();
      }
    });
  }

  @Override
  public void dispose() {
    EditorWindowImpl.disposeInvalidEditors();
  }

  @Nonnull
  @Override
  public List<DocumentWindow> getCachedInjectedDocuments(@Nonnull PsiFile hostPsiFile) {
    return InjectedLanguageUtil.getCachedInjectedDocuments(hostPsiFile);
  }

  @Override
  public void enumerate(@Nonnull PsiElement host, @Nonnull PsiLanguageInjectionHost.InjectedPsiVisitor visitor) {
    InjectedLanguageUtil.enumerate(host, visitor);
  }

  @Override
  public void enumerateEx(@Nonnull PsiElement host,
                          @Nonnull PsiFile containingFile,
                          boolean probeUp,
                          @Nonnull PsiLanguageInjectionHost.InjectedPsiVisitor visitor) {
    InjectedLanguageUtil.enumerate(host, containingFile, probeUp, visitor);
  }

  @Override
  public void startRunInjectors(@Nonnull final Document hostDocument, final boolean synchronously) {
    if (myProject.isDisposed()) return;
    if (!synchronously && ApplicationManager.getApplication().isWriteAccessAllowed()) return;
    // use cached to avoid recreate PSI in alien project
    final PsiDocumentManager documentManager = PsiDocumentManager.getInstance(myProject);
    final PsiFile hostPsiFile = documentManager.getCachedPsiFile(hostDocument);
    if (hostPsiFile == null) return;

    final ConcurrentList<DocumentWindow> injected = InjectedLanguageUtil.getCachedInjectedDocuments(hostPsiFile);
    if (injected.isEmpty()) return;

    if (myProgress.isCanceled()) {
      myProgress = new DaemonProgressIndicator();
    }
    final Set<DocumentWindow> newDocuments = Collections.synchronizedSet(new THashSet<>());
    final Processor<DocumentWindow> commitProcessor = documentWindow -> {
      if (myProject.isDisposed()) return false;
      ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
      if (indicator != null && indicator.isCanceled()) return false;
      if (documentManager.isUncommited(hostDocument) || !hostPsiFile.isValid()) return false; // will be committed later

      // it is here where the reparse happens and old file contents replaced
      InjectedLanguageUtil.enumerate(documentWindow, hostPsiFile, (injectedPsi, places) -> {
        DocumentWindow newDocument = (DocumentWindow)injectedPsi.getViewProvider().getDocument();
        if (newDocument != null) {
          PsiDocumentManagerBase.checkConsistency(injectedPsi, newDocument);
          newDocuments.add(newDocument);
        }
      });
      return true;
    };
    final Runnable commitInjectionsRunnable = () -> {
      if (myProgress.isCanceled()) return;
      JobLauncher.getInstance().invokeConcurrentlyUnderProgress(new ArrayList<>(injected), myProgress, true, commitProcessor);

      synchronized (ourInjectionPsiLock) {
        injected.clear();
        injected.addAll(newDocuments);
      }
    };

    if (synchronously) {
      commitInjectionsRunnable.run();
    }
    else {
      JobLauncher.getInstance().submitToJobThread(() -> ApplicationManagerEx.getApplicationEx().tryRunReadAction(commitInjectionsRunnable), null);
    }
  }

  @Nonnull
  @Override
  public List<DocumentWindow> getCachedInjectedDocumentsInRange(@Nonnull PsiFile hostPsiFile, @Nonnull TextRange range) {
    return InjectedLanguageUtil.getCachedInjectedDocumentsInRange(hostPsiFile, range);
  }

  public void psiManagerInjectorsChanged() {
    LanguageInjector[] extensions = Extensions.getExtensions(LanguageInjector.EXTENSION_POINT_NAME);
    if (extensions.length == 0) {
      MultiHostInjector prev = myPsiManagerRegisteredInjectorsAdapter.getAndSet(null);
      if (prev != null) {
        unregisterMultiHostInjector(prev);
      }
    }
    else {
      PsiManagerRegisteredInjectorsAdapter adapter = new PsiManagerRegisteredInjectorsAdapter();
      if (myPsiManagerRegisteredInjectorsAdapter.compareAndSet(null, adapter)) {
        registerMultiHostInjector(adapter, PsiLanguageInjectionHost.class);
      }
    }
  }

  @Nonnull
  @Override
  public List<TextRange> getNonEditableFragments(@Nonnull DocumentWindow window) {
    List<TextRange> result = ContainerUtil.newArrayList();
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

  private static int appendRange(List<TextRange> result, int start, int length) {
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

  @Override
  public PsiLanguageInjectionHost getInjectionHost(@Nonnull PsiElement element) {
    final PsiFile file = element.getContainingFile();
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
    ProperTextRange.assertProperRange(injectedTextRange);
    PsiFile file = injectedContext.getContainingFile();
    if (file == null) return injectedTextRange;
    Document document = PsiDocumentManager.getInstance(file.getProject()).getCachedDocument(file);
    if (!(document instanceof DocumentWindowImpl)) return injectedTextRange;
    DocumentWindowImpl documentWindow = (DocumentWindowImpl)document;
    return documentWindow.injectedToHost(injectedTextRange);
  }

  @Override
  public int injectedToHost(@Nonnull PsiElement element, int offset) {
    PsiFile file = element.getContainingFile();
    if (file == null) return offset;
    Document document = PsiDocumentManager.getInstance(file.getProject()).getCachedDocument(file);
    if (!(document instanceof DocumentWindowImpl)) return offset;
    DocumentWindowImpl documentWindow = (DocumentWindowImpl)document;
    return documentWindow.injectedToHost(offset);
  }

  // used only from tests => no need for complex synchronization
  private final MultiMap<Class<? extends PsiElement>, MultiHostInjector> myManualInjectors = MultiMap.createLinkedSet();
  private volatile ClassMapCachingNulls<MultiHostInjector> cachedInjectors;

  public void processInjectableElements(Collection<PsiElement> in, Processor<PsiElement> processor) {
    ClassMapCachingNulls<MultiHostInjector> map = getInjectorMap();
    for (PsiElement element : in) {
      if (map.get(element.getClass()) != null) processor.process(element);
    }
  }

  private ClassMapCachingNulls<MultiHostInjector> getInjectorMap() {
    ClassMapCachingNulls<MultiHostInjector> cached = cachedInjectors;
    if (cached != null) {
      return cached;
    }

    Map<Class, MultiHostInjector[]> injectors = ContainerUtil.newHashMap();

    MultiMap<Class<? extends PsiElement>, MultiHostInjector> allInjectors = MultiMap.createLinkedSet();
    allInjectors.putAllValues(myManualInjectors);
    for (MultiHostInjectorExtensionPoint multiHostInjectorExtensionPoint : MultiHostInjector.EP_NAME.getExtensions(myProject)) {
      Class<PsiElement> key = multiHostInjectorExtensionPoint.getKey();
      MultiHostInjector multiHostInjector = multiHostInjectorExtensionPoint.getInstance(myProject);
      allInjectors.putValue(key, multiHostInjector);
    }
    if (LanguageInjector.EXTENSION_POINT_NAME.getExtensions().length > 0) {
      allInjectors.putValue(PsiLanguageInjectionHost.class, PsiManagerRegisteredInjectorsAdapter.INSTANCE);
    }

    for (Map.Entry<Class<? extends PsiElement>, Collection<MultiHostInjector>> entry : allInjectors.entrySet()) {
      Class<? extends PsiElement> place = entry.getKey();
      Collection<MultiHostInjector> value = entry.getValue();

      for (MultiHostInjector injector : value) {
        MultiHostInjector[] existing = injectors.get(place);
        injectors.put(place, existing == null ? new MultiHostInjector[]{injector} : ArrayUtil.append(existing, injector));
      }
    }

    ClassMapCachingNulls<MultiHostInjector> result = new ClassMapCachingNulls<MultiHostInjector>(injectors, new MultiHostInjector[0]);
    cachedInjectors = result;
    return result;
  }

  private void clearInjectorCache() {
    cachedInjectors = null;
  }

  @Override
  public void registerMultiHostInjector(@Nonnull MultiHostInjector injector, @Nonnull Class<? extends PsiElement>... elements) {
    for (Class<? extends PsiElement> element : elements) {
      myManualInjectors.putValue(element, injector);
    }
    clearInjectorCache();
  }

  @Override
  public boolean unregisterMultiHostInjector(@Nonnull MultiHostInjector injector) {
    try {
      return myManualInjectors.values().remove(injector);
    }
    finally {
      clearInjectorCache();
    }
  }


  @Override
  public String getUnescapedText(@Nonnull final PsiElement injectedNode) {
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
  @Override
  @SuppressWarnings({"ConstantConditions", "unchecked"})
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
            List<TextRange> list = new ArrayList<TextRange>();
            list.add(range);
            list.add(intersection);
            result = list;
          }
        }
        else if (intersection.isEmpty()) {
          count--;
        }
        else {
          ((List<TextRange>)result).add(intersection);
        }
      }
      offset += shred.getPrefix().length() + shred.getRangeInsideHost().getLength() + shred.getSuffix().length();
    }
    return count == 0 ? Collections.<TextRange>emptyList() : count == 1 ? Collections.singletonList((TextRange)result) : (List<TextRange>)result;
  }

  @Override
  public boolean isInjectedFragment(final PsiFile file) {
    return file.getViewProvider() instanceof InjectedFileViewProvider;
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

  private final Map<Class, MultiHostInjector[]> myInjectorsClone = new HashMap<Class, MultiHostInjector[]>();

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
  public static void checkInjectorsAreDisposed(@Nonnull Project project) {
    InjectedLanguageManagerImpl cachedManager = (InjectedLanguageManagerImpl)project.getUserData(INSTANCE_CACHE);
    if (cachedManager == null) {
      return;
    }
    try {
      ClassMapCachingNulls<MultiHostInjector> cached = cachedManager.cachedInjectors;
      if (cached == null) return;
      for (Map.Entry<Class, MultiHostInjector[]> entry : cached.getBackingMap().entrySet()) {
        Class key = entry.getKey();
        if (cachedManager.myInjectorsClone.isEmpty()) return;
        MultiHostInjector[] oldInjectors = cachedManager.myInjectorsClone.get(key);
        for (MultiHostInjector injector : entry.getValue()) {
          if (!ArrayUtil.contains(injector, oldInjectors)) {
            throw new AssertionError("Injector was not disposed: " + key + " -> " + injector);
          }
        }
      }
    }
    finally {
      cachedManager.myInjectorsClone.clear();
    }
  }

  public interface InjProcessor {
    boolean process(PsiElement element, MultiHostInjector injector);
  }

  public void processInPlaceInjectorsFor(@Nonnull PsiElement element, @Nonnull InjProcessor processor) {
    MultiHostInjector[] infos = getInjectorMap().get(element.getClass());
    if (infos != null) {
      final boolean dumb = myDumbService.isDumb();
      for (MultiHostInjector injector : infos) {
        if (dumb && !DumbService.isDumbAware(injector)) {
          continue;
        }

        if (!processor.process(element, injector)) return;
      }
    }
  }

  @Override
  @Nullable
  public List<Pair<PsiElement, TextRange>> getInjectedPsiFiles(@Nonnull final PsiElement host) {
    if (!(host instanceof PsiLanguageInjectionHost) || !((PsiLanguageInjectionHost)host).isValidHost()) {
      return null;
    }
    final PsiElement inTree = InjectedLanguageUtil.loadTree(host, host.getContainingFile());
    final List<Pair<PsiElement, TextRange>> result = new SmartList<Pair<PsiElement, TextRange>>();
    InjectedLanguageUtil.enumerate(inTree, new PsiLanguageInjectionHost.InjectedPsiVisitor() {
      @Override
      public void visit(@Nonnull PsiFile injectedPsi, @Nonnull List<PsiLanguageInjectionHost.Shred> places) {
        for (PsiLanguageInjectionHost.Shred place : places) {
          if (place.getHost() == inTree) {
            result.add(new Pair<PsiElement, TextRange>(injectedPsi, place.getRangeInsideHost()));
          }
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
      InjectedLanguagePlaces placesRegistrar = new InjectedLanguagePlaces() {
        @Override
        public void addPlace(@Nonnull Language language, @Nonnull TextRange rangeInsideHost, @NonNls @Nullable String prefix, @NonNls @Nullable String suffix) {
          ProperTextRange.assertProperRange(rangeInsideHost);
          injectionPlacesRegistrar.startInjecting(language).addPlace(prefix, suffix, host, rangeInsideHost).doneInjecting();
        }
      };
      for (LanguageInjector injector : Extensions.getExtensions(LanguageInjector.EXTENSION_POINT_NAME)) {
        injector.getLanguagesToInject(host, placesRegistrar);
      }
    }
  }
}
