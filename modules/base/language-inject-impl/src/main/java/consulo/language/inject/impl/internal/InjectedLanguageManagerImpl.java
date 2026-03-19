// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.language.inject.impl.internal;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ServiceImpl;
import consulo.application.progress.ProgressIndicator;
import consulo.disposer.Disposable;
import consulo.document.Document;
import consulo.document.DocumentWindow;
import consulo.document.internal.DocumentEx;
import consulo.document.util.Segment;
import consulo.document.util.TextRange;
import consulo.language.Language;
import consulo.language.ast.ASTNode;
import consulo.language.file.FileViewProvider;
import consulo.language.file.inject.VirtualFileWindow;
import consulo.language.impl.internal.psi.PsiDocumentManagerBase;
import consulo.language.inject.*;
import consulo.language.internal.InjectedHighlightTokenInfo;
import consulo.language.internal.InjectedLanguageManagerInternal;
import consulo.language.psi.*;
import consulo.logging.Logger;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.MultiMap;
import consulo.util.collection.SmartList;
import consulo.util.dataholder.Key;
import consulo.util.lang.Pair;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * @author cdr
 */
@Singleton
@ServiceImpl
public class InjectedLanguageManagerImpl implements InjectedLanguageManagerInternal, Disposable {
    private static final Logger LOG = Logger.getInstance(InjectedLanguageManagerImpl.class);
    @SuppressWarnings("RedundantStringConstructorCall")
    static final Object ourInjectionPsiLock = new String("injectionPsiLock");
    private final Project myProject;
    private final DumbService myDumbService;
    private final PsiDocumentManager myDocManager;

    public static InjectedLanguageManagerImpl getInstanceImpl(Project project) {
        return (InjectedLanguageManagerImpl) InjectedLanguageManager.getInstance(project);
    }

    @Inject
    public InjectedLanguageManagerImpl(Project project,
                                       DumbService dumbService,
                                       PsiDocumentManager psiDocumentManager) {
        myProject = project;
        myDumbService = dumbService;
        myDocManager = psiDocumentManager;
    }

    @Override
    public void dispose() {
        disposeInvalidEditors();
    }

    @Override
    public void disposeInvalidEditors() {
        EditorWindowImpl.disposeInvalidEditors();
    }

    @Override
    public PsiLanguageInjectionHost getInjectionHost(FileViewProvider injectedProvider) {
        if (!(injectedProvider instanceof InjectedFileViewProvider)) {
            return null;
        }
        return ((InjectedFileViewProvider) injectedProvider).getShreds().getHostPointer().getElement();
    }

    @Override
    public List<InjectedHighlightTokenInfo> getHighlightTokens(PsiFile file) {
        return InjectedLanguageUtil.getHighlightTokens(file);
    }

    @Override
    public PsiLanguageInjectionHost getInjectionHost(PsiElement injectedElement) {
        PsiFile file = injectedElement.getContainingFile();
        VirtualFile virtualFile = file == null ? null : file.getVirtualFile();
        if (virtualFile instanceof VirtualFileWindow) {
            PsiElement host = FileContextUtil.getFileContext(file); // use utility method in case the file's overridden getContext()
            if (host instanceof PsiLanguageInjectionHost) {
                return (PsiLanguageInjectionHost) host;
            }
        }
        return null;
    }

    @Override
    
    public TextRange injectedToHost(PsiElement injectedContext, TextRange injectedTextRange) {
        DocumentWindow documentWindow = getDocumentWindow(injectedContext);
        return documentWindow == null ? injectedTextRange : documentWindow.injectedToHost(injectedTextRange);
    }

    @Override
    public int injectedToHost(PsiElement element, int offset) {
        DocumentWindow documentWindow = getDocumentWindow(element);
        return documentWindow == null ? offset : documentWindow.injectedToHost(offset);
    }

    @Override
    public int injectedToHost(PsiElement injectedContext, int injectedOffset, boolean minHostOffset) {
        DocumentWindow documentWindow = getDocumentWindow(injectedContext);
        return documentWindow == null ? injectedOffset : documentWindow.injectedToHost(injectedOffset, minHostOffset);
    }

    @Override
    public DocumentWindow getDocumentWindow(PsiElement element) {
        PsiFile file = element.getContainingFile();
        if (file == null) {
            return null;
        }
        Document document = PsiDocumentManager.getInstance(file.getProject()).getCachedDocument(file);
        return document instanceof DocumentWindow documentWindow ? documentWindow : null;
    }

    // used only from tests => no need for complex synchronization
    private volatile ClassMapCachingNulls<MultiHostInjector> cachedInjectors;

    @Override
    public void processInjectableElements(Collection<? extends PsiElement> in, Predicate<? super PsiElement> processor) {
        ClassMapCachingNulls<MultiHostInjector> map = getInjectorMap();
        for (PsiElement element : in) {
            if (map.get(element.getClass()) != null) {
                processor.test(element);
            }
        }
    }

    
    private ClassMapCachingNulls<MultiHostInjector> getInjectorMap() {
        ClassMapCachingNulls<MultiHostInjector> cached = cachedInjectors;
        if (cached != null) {
            return cached;
        }

        ClassMapCachingNulls<MultiHostInjector> result = calcInjectorMap();
        cachedInjectors = result;
        return result;
    }

    
    private ClassMapCachingNulls<MultiHostInjector> calcInjectorMap() {
        Map<Class<?>, MultiHostInjector[]> injectors = new HashMap<>();

        MultiMap<Class<? extends PsiElement>, MultiHostInjector> allInjectors = new MultiMap<>();
        for (MultiHostInjector multiHostInjector : myProject.getExtensionList(MultiHostInjector.class)) {
            allInjectors.putValue(multiHostInjector.getElementClass(), multiHostInjector);
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

    @Override
    public @Nullable String getUnescapedLeafText(PsiElement element, boolean strict) {
        return InjectedLanguageUtil.getUnescapedLeafText(element, strict);
    }

    
    @Override
    public String getUnescapedText(PsiElement injectedNode) {
        String leafText = InjectedLanguageUtil.getUnescapedLeafText(injectedNode, false);
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
    
    public List<TextRange> intersectWithAllEditableFragments(PsiFile injectedPsi, TextRange rangeToEdit) {
        PlaceImpl shreds = InjectedLanguageUtil.getShreds(injectedPsi);
        if (shreds == null) {
            return Collections.emptyList();
        }
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
                    TextRange range = (TextRange) result;
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
                    ((List<TextRange>) result).add(intersection);
                }
            }
            offset += shred.getPrefix().length() + shred.getRangeInsideHost().getLength() + shred.getSuffix().length();
        }
        //noinspection unchecked
        return count == 0 ? Collections.emptyList() : count == 1 ? Collections.singletonList((TextRange) result) : (List<TextRange>) result;
    }

    @Override
    public boolean isInjectedFragment(PsiFile injectedFile) {
        return injectedFile.getViewProvider() instanceof InjectedFileViewProvider;
    }

    @Override
    public PsiFile findInjectedPsiNoCommit(PsiFile host, int offset) {
        return InjectedLanguageUtil.findInjectedPsiNoCommit(host, offset);
    }

    @Override
    public PsiElement findInjectedElementAt(PsiFile hostFile, int hostDocumentOffset) {
        return InjectedLanguageUtil.findInjectedElementNoCommit(hostFile, hostDocumentOffset);
    }

    @Nullable
    @Override
    public PsiElement findElementAtNoCommit(PsiFile file, int offset) {
        return InjectedLanguageUtil.findElementAtNoCommit(file, offset);
    }

    @Override
    public void dropFileCaches(PsiFile file) {
        InjectedLanguageUtil.clearCachedInjectedFragmentsForFile(file);
    }

    @Override
    public PsiFile getTopLevelFile(PsiElement element) {
        return InjectedLanguageUtil.getTopLevelFile(element);
    }

    
    @Override
    public List<DocumentWindow> getCachedInjectedDocumentsInRange(PsiFile hostPsiFile, TextRange range) {
        return InjectedLanguageUtil.getCachedInjectedDocumentsInRange(hostPsiFile, range);
    }

    @Override
    public void enumerate(PsiElement host, PsiLanguageInjectionHost.InjectedPsiVisitor visitor) {
        InjectedLanguageUtil.enumerate(host, visitor);
    }

    @Override
    public void enumerate(DocumentWindow documentWindow, PsiFile hostPsiFile, PsiLanguageInjectionHost.InjectedPsiVisitor visitor) {
        InjectedLanguageUtil.enumerate(documentWindow, hostPsiFile, visitor);
    }

    @RequiredReadAction
    @Override
    public void enumerateEx(PsiElement host, PsiFile containingFile, boolean probeUp, PsiLanguageInjectionHost.InjectedPsiVisitor visitor) {
        InjectedLanguageUtil.enumerate(host, containingFile, probeUp, visitor);
    }

    
    @Override
    public List<TextRange> getNonEditableFragments(DocumentWindow window) {
        List<TextRange> result = new ArrayList<>();
        int offset = 0;
        for (PsiLanguageInjectionHost.Shred shred : ((DocumentWindowImpl) window).getShreds()) {
            Segment hostRange = shred.getHostRangeMarker();
            if (hostRange == null) {
                continue;
            }

            offset = appendRange(result, offset, shred.getPrefix().length());
            offset += hostRange.getEndOffset() - hostRange.getStartOffset();
            offset = appendRange(result, offset, shred.getSuffix().length());
        }

        return result;
    }

    @Override
    public boolean mightHaveInjectedFragmentAtOffset(Document hostDocument, int hostOffset) {
        return InjectedLanguageUtil.mightHaveInjectedFragmentAtCaret(myProject, hostDocument, hostOffset);
    }

    
    @Override
    public DocumentWindow freezeWindow(DocumentWindow document) {
        PlaceImpl shreds = ((DocumentWindowImpl) document).getShreds();
        Project project = shreds.getHostPointer().getProject();
        DocumentEx delegate = ((PsiDocumentManagerBase) PsiDocumentManager.getInstance(project)).getLastCommittedDocument(document.getDelegate());
        PlaceImpl place = new PlaceImpl();
        place.addAll(ContainerUtil.map(shreds, shred -> ((ShredImpl) shred).withPsiRange()));
        return new DocumentWindowImpl(delegate, place);
    }

    @Override
    public PsiLanguageInjectionHost.@Nullable Place getShreds(PsiFile injectedFile) {
        return InjectedLanguageUtil.getShreds(injectedFile);
    }

    @Override
    public PsiLanguageInjectionHost.@Nullable Place getShreds(FileViewProvider viewProvider) {
        return InjectedLanguageUtil.getShreds(viewProvider);
    }

    
    @Override
    public PsiLanguageInjectionHost.Place getShreds(DocumentWindow documentWindow) {
        return ((DocumentWindowImpl) documentWindow).getShreds();
    }

    @Override
    public void injectLanguagesFromConcatenationAdapter(MultiHostRegistrar registrar,
                                                           PsiElement context,
                                                           Function<PsiElement, Pair<PsiElement, PsiElement[]>> computeAnchorAndOperandsFunc) {
        ConcatenationInjectorManager.getInstance(myProject).injectLanguagesFromConcatenationAdapter(registrar, context, computeAnchorAndOperandsFunc);
    }

    @Override
    public int hostToInjectedUnescaped(DocumentWindow window, int hostOffset) {
        return InjectedLanguageUtil.hostToInjectedUnescaped(window, hostOffset);
    }

    @Override
    public void injectReference(MultiHostRegistrar registrar, Language language, String prefix, String suffix, PsiLanguageInjectionHost host, TextRange rangeInsideHost) {
        InjectedLanguageUtil.injectReference(registrar, language, prefix, suffix, host, rangeInsideHost);
    }

    @Override
    public <T> void putInjectedFileUserData(PsiElement element, Language language, Key<T> key, @Nullable T value) {
        InjectedLanguageUtil.putInjectedFileUserData(element, language, key, value);
    }

    @Override
    
    @RequiredReadAction
    public String getUnescapedText(PsiFile file, @Nullable PsiElement startElement, @Nullable PsiElement endElement) {
        return InjectedLanguageUtil.getUnescapedText(file, startElement, endElement);
    }

    @Override
    public BooleanSupplier reparse(PsiFile injectedPsiFile,
                                   DocumentWindow injectedDocument,
                                   PsiFile hostPsiFile,
                                   Document hostDocument,
                                   FileViewProvider hostViewProvider,
                                   ProgressIndicator indicator,
                                   ASTNode oldRoot,
                                   ASTNode newRoot) {
        return InjectedLanguageUtil.reparse(injectedPsiFile, injectedDocument, hostPsiFile, hostDocument, hostViewProvider, indicator, oldRoot, newRoot, (PsiDocumentManagerBase) myDocManager);
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

    private final Map<Class<?>, MultiHostInjector[]> myInjectorsClone = new HashMap<>();

    //@TestOnly
    //public static void pushInjectors(Project project) {
    //  InjectedLanguageManagerImpl cachedManager = (InjectedLanguageManagerImpl)project.getUserData(INSTANCE_CACHE);
    //  if (cachedManager == null) return;
    //  try {
    //    assert cachedManager.myInjectorsClone.isEmpty() : cachedManager.myInjectorsClone;
    //  }
    //  finally {
    //    cachedManager.myInjectorsClone.clear();
    //  }
    //  cachedManager.myInjectorsClone.putAll(cachedManager.getInjectorMap().getBackingMap());
    //}
    //
    //@TestOnly
    //public static void checkInjectorsAreDisposed(@Nullable Project project) {
    //  InjectedLanguageManagerImpl cachedManager = project == null ? null : (InjectedLanguageManagerImpl)project.getUserData(INSTANCE_CACHE);
    //  if (cachedManager == null) return;
    //
    //  try {
    //    ClassMapCachingNulls<MultiHostInjector> cached = cachedManager.cachedInjectors;
    //    if (cached == null) return;
    //    for (Map.Entry<Class<?>, MultiHostInjector[]> entry : cached.getBackingMap().entrySet()) {
    //      Class<?> key = entry.getKey();
    //      if (cachedManager.myInjectorsClone.isEmpty()) return;
    //      MultiHostInjector[] oldInjectors = cachedManager.myInjectorsClone.get(key);
    //      for (MultiHostInjector injector : entry.getValue()) {
    //        if (ArrayUtil.indexOf(oldInjectors, injector) == -1) {
    //          throw new AssertionError("Injector was not disposed: " + key + " -> " + injector);
    //        }
    //      }
    //    }
    //  }
    //  finally {
    //    cachedManager.myInjectorsClone.clear();
    //  }
    //}

    InjectionResult processInPlaceInjectorsFor(PsiFile hostPsiFile, PsiElement element) {
        MultiHostInjector[] infos = getInjectorMap().get(element.getClass());
        if (infos == null || infos.length == 0) {
            return null;
        }
        boolean dumb = myDumbService.isDumb();
        InjectionRegistrarImpl hostRegistrar = new InjectionRegistrarImpl(myProject, hostPsiFile, element, myDocManager);
        for (MultiHostInjector injector : infos) {
            if (dumb && !DumbService.isDumbAware(injector)) {
                continue;
            }

            injector.injectLanguages(hostRegistrar, element);
            InjectionResult result = hostRegistrar.getInjectedResult();
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    @Override
    public @Nullable List<Pair<PsiElement, TextRange>> getInjectedPsiFiles(PsiElement host) {
        if (!(host instanceof PsiLanguageInjectionHost) || !((PsiLanguageInjectionHost) host).isValidHost()) {
            return null;
        }
        PsiElement inTree = InjectedLanguageUtil.loadTree(host, host.getContainingFile());
        List<Pair<PsiElement, TextRange>> result = new SmartList<>();
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
        public Class<? extends PsiElement> getElementClass() {
            return PsiLanguageInjectionHost.class;
        }

        @Override
        public void injectLanguages(MultiHostRegistrar injectionPlacesRegistrar, PsiElement context) {
            PsiLanguageInjectionHost host = (PsiLanguageInjectionHost) context;
            InjectedLanguagePlaces placesRegistrar =
                (language, rangeInsideHost, prefix, suffix) -> injectionPlacesRegistrar.startInjecting(language).addPlace(prefix, suffix, host, rangeInsideHost).doneInjecting();

            LanguageInjector.EXTENSION_POINT_NAME.forEachExtensionSafe(injector -> injector.injectLanguages(host, placesRegistrar));
        }
    }
}
