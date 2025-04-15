// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.language.editor.impl.inspection.reference;

import consulo.application.ApplicationManager;
import consulo.application.ReadAction;
import consulo.application.dumb.IndexNotReadyException;
import consulo.application.progress.ProgressManager;
import consulo.application.util.registry.Registry;
import consulo.component.ProcessCanceledException;
import consulo.document.Document;
import consulo.document.util.Segment;
import consulo.language.Language;
import consulo.language.editor.inspection.GlobalInspectionContext;
import consulo.language.editor.inspection.InspectionExtensionsFactory;
import consulo.language.editor.inspection.InspectionsBundle;
import consulo.language.editor.inspection.reference.*;
import consulo.language.editor.scope.AnalysisScope;
import consulo.language.file.FileViewProvider;
import consulo.language.impl.psi.PsiAnchor;
import consulo.language.inject.InjectedLanguageManager;
import consulo.language.psi.*;
import consulo.logging.Logger;
import consulo.logging.attachment.AttachmentFactory;
import consulo.logging.attachment.RuntimeExceptionWithAttachments;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.module.content.util.ProjectUtilCore;
import consulo.project.Project;
import consulo.project.macro.ProjectPathMacroManager;
import consulo.util.collection.Lists;
import consulo.util.collection.Maps;
import consulo.util.dataholder.Key;
import consulo.util.interner.Interner;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import consulo.virtualFileSystem.VirtualFileWithId;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import org.jdom.Element;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class RefManagerImpl extends RefManager {
    private static final Logger LOG = Logger.getInstance(RefManagerImpl.class);

    private long myLastUsedMask = 0x0800_0000; // guarded by this

    @Nonnull
    private final Project myProject;
    private AnalysisScope myScope;
    private RefProject myRefProject;

    private final BitSet myUnprocessedFiles = new BitSet();
    private final boolean processExternalElements = Registry.is("batch.inspections.process.external.elements");
    private final ConcurrentMap<PsiAnchor, RefElement> myRefTable = new ConcurrentHashMap<>();

    private volatile List<RefElement> myCachedSortedRefs;
    // holds cached values from myPsiToRefTable/myRefTable sorted by containing virtual file; benign data race

    private final ConcurrentMap<Module, RefModule> myModules = new ConcurrentHashMap<>();
    private final ProjectIterator myProjectIterator = new ProjectIterator();
    private final AtomicBoolean myDeclarationsFound = new AtomicBoolean(false);
    private final PsiManager myPsiManager;

    private volatile boolean myIsInProcess;
    private volatile boolean myOfflineView;

    private final LinkedHashSet<RefGraphAnnotator> myGraphAnnotators = new LinkedHashSet<>();
    private GlobalInspectionContext myContext;

    private final Map<Key, RefManagerExtension> myExtensions = new HashMap<>();
    private final Map<Language, RefManagerExtension> myLanguageExtensions = new HashMap<>();
    private final Interner<String> myNameInterner = consulo.util.interner.Interner.createStringInterner();

    public RefManagerImpl(@Nonnull Project project, @Nullable AnalysisScope scope, @Nonnull GlobalInspectionContext context) {
        myProject = project;
        myScope = scope;
        myContext = context;
        myPsiManager = PsiManager.getInstance(project);
        myRefProject = new RefProjectImpl(this);
        for (InspectionExtensionsFactory factory : InspectionExtensionsFactory.EP_NAME.getExtensionList()) {
            final RefManagerExtension<?> extension = factory.createRefManagerExtension(this);
            if (extension != null) {
                myExtensions.put(extension.getID(), extension);
                for (Language language : extension.getLanguages()) {
                    myLanguageExtensions.put(language, extension);
                }
            }
        }
        if (scope != null) {
            for (Module module : ModuleManager.getInstance(getProject()).getModules()) {
                getRefModule(module);
            }
        }
    }

    String internName(@Nonnull String name) {
        synchronized (myNameInterner) {
            return myNameInterner.intern(name);
        }
    }

    @Nonnull
    public GlobalInspectionContext getContext() {
        return myContext;
    }

    @Override
    public void iterate(@Nonnull RefVisitor visitor) {
        for (RefElement refElement : getSortedElements()) {
            refElement.accept(visitor);
        }
        for (RefModule refModule : myModules.values()) {
            if (myScope.containsModule(refModule.getModule())) {
                refModule.accept(visitor);
            }
        }
        for (RefManagerExtension extension : myExtensions.values()) {
            extension.iterate(visitor);
        }
    }

    public void cleanup() {
        myScope = null;
        myRefProject = null;
        myRefTable.clear();
        myCachedSortedRefs = null;
        myModules.clear();
        myContext = null;

        myGraphAnnotators.clear();
        for (RefManagerExtension extension : myExtensions.values()) {
            extension.cleanup();
        }
        myExtensions.clear();
        myLanguageExtensions.clear();
    }

    @Nullable
    @Override
    public AnalysisScope getScope() {
        return myScope;
    }


    private void fireNodeInitialized(RefElement refElement) {
        for (RefGraphAnnotator annotator : myGraphAnnotators) {
            annotator.onInitialize(refElement);
        }
    }

    public void fireNodeMarkedReferenced(
        RefElement refWhat,
        RefElement refFrom,
        boolean referencedFromClassInitializer,
        final boolean forReading,
        final boolean forWriting
    ) {
        for (RefGraphAnnotator annotator : myGraphAnnotators) {
            annotator.onMarkReferenced(refWhat, refFrom, referencedFromClassInitializer, forReading, forWriting);
        }
    }

    public void fireNodeMarkedReferenced(
        RefElement refWhat,
        RefElement refFrom,
        boolean referencedFromClassInitializer,
        final boolean forReading,
        final boolean forWriting,
        PsiElement element
    ) {
        for (RefGraphAnnotator annotator : myGraphAnnotators) {
            annotator.onMarkReferenced(refWhat, refFrom, referencedFromClassInitializer, forReading, forWriting, element);
        }
    }

    public void fireNodeMarkedReferenced(PsiElement what, PsiElement from) {
        for (RefGraphAnnotator annotator : myGraphAnnotators) {
            annotator.onMarkReferenced(what, from, false);
        }
    }

    public void fireBuildReferences(RefElement refElement) {
        for (RefGraphAnnotator annotator : myGraphAnnotators) {
            annotator.onReferencesBuild(refElement);
        }
    }

    public void registerGraphAnnotator(@Nonnull RefGraphAnnotator annotator) {
        if (myGraphAnnotators.add(annotator) && annotator instanceof RefGraphAnnotatorEx) {
            ((RefGraphAnnotatorEx)annotator).initialize(this);
        }
    }

    @Override
    public synchronized long getLastUsedMask() {
        if (myLastUsedMask < 0) {
            throw new IllegalStateException("We're out of 64 bits, sorry");
        }
        myLastUsedMask *= 2;
        return myLastUsedMask;
    }

    @Override
    public <T> T getExtension(@Nonnull final Key<T> key) {
        //noinspection unchecked
        return (T)myExtensions.get(key);
    }

    @Override
    @Nullable
    public String getType(@Nonnull final RefEntity ref) {
        for (RefManagerExtension extension : myExtensions.values()) {
            final String type = extension.getType(ref);
            if (type != null) {
                return type;
            }
        }
        if (ref instanceof RefFile) {
            return SmartRefElementPointer.FILE;
        }
        if (ref instanceof RefModule) {
            return SmartRefElementPointer.MODULE;
        }
        if (ref instanceof RefProject) {
            return SmartRefElementPointer.PROJECT;
        }
        if (ref instanceof RefDirectory) {
            return SmartRefElementPointer.DIR;
        }
        return null;
    }

    @Nonnull
    @Override
    public RefEntity getRefinedElement(@Nonnull RefEntity ref) {
        for (RefManagerExtension extension : myExtensions.values()) {
            ref = extension.getRefinedElement(ref);
        }
        return ref;
    }

    @Nullable
    @Override
    public Element export(@Nonnull RefEntity refEntity, final int actualLine) {
        refEntity = getRefinedElement(refEntity);

        Element problem = new Element("problem");

        if (refEntity instanceof RefDirectory) {
            Element fileElement = new Element("file");
            VirtualFile virtualFile = ((PsiDirectory)((RefDirectory)refEntity).getPsiElement()).getVirtualFile();
            fileElement.addContent(virtualFile.getUrl());
            problem.addContent(fileElement);
        }
        else if (refEntity instanceof RefElement) {
            final RefElement refElement = (RefElement)refEntity;
            final SmartPsiElementPointer pointer = refElement.getPointer();
            PsiFile psiFile = pointer.getContainingFile();
            if (psiFile == null) {
                return null;
            }

            Element fileElement = new Element("file");
            Element lineElement = new Element("line");
            final VirtualFile virtualFile = psiFile.getVirtualFile();
            LOG.assertTrue(virtualFile != null);
            fileElement.addContent(virtualFile.getUrl());

            if (actualLine == -1) {
                final Document document = PsiDocumentManager.getInstance(pointer.getProject()).getDocument(psiFile);
                LOG.assertTrue(document != null);
                final Segment range = pointer.getRange();
                lineElement.addContent(String.valueOf(range != null ? document.getLineNumber(range.getStartOffset()) + 1 : -1));
            }
            else {
                lineElement.addContent(String.valueOf(actualLine + 1));
            }

            problem.addContent(fileElement);
            problem.addContent(lineElement);

            appendModule(problem, refElement.getModule());
        }
        else if (refEntity instanceof RefModule) {
            final RefModule refModule = (RefModule)refEntity;
            final VirtualFile moduleDir = refModule.getModule().getModuleDir();
            final Element fileElement = new Element("file");
            fileElement.addContent(moduleDir != null ? moduleDir.getUrl() : refEntity.getName());
            problem.addContent(fileElement);
            appendModule(problem, refModule);
        }

        for (RefManagerExtension extension : myExtensions.values()) {
            extension.export(refEntity, problem);
        }

        new SmartRefElementPointerImpl(refEntity, true).writeExternal(problem);
        return problem;
    }

    @Override
    @Nullable
    public String getGroupName(@Nonnull final RefElement entity) {
        for (RefManagerExtension extension : myExtensions.values()) {
            final String groupName = extension.getGroupName(entity);
            if (groupName != null) {
                return groupName;
            }
        }

        RefEntity parent = entity.getOwner();
        while (parent != null && !(parent instanceof RefDirectory)) {
            parent = parent.getOwner();
        }
        final LinkedList<String> containingDirs = new LinkedList<>();
        while (parent instanceof RefDirectory) {
            containingDirs.addFirst(parent.getName());
            parent = parent.getOwner();
        }
        return containingDirs.isEmpty() ? null : StringUtil.join(containingDirs, File.separator);
    }

    private static void appendModule(final Element problem, final RefModule refModule) {
        if (refModule != null) {
            Element moduleElement = new Element("module");
            moduleElement.addContent(refModule.getName());
            problem.addContent(moduleElement);
        }
    }

    public void findAllDeclarations() {
        if (!myDeclarationsFound.getAndSet(true)) {
            long before = System.currentTimeMillis();
            final AnalysisScope scope = getScope();
            if (scope != null) {
                scope.accept(myProjectIterator);
            }

            LOG.info("Total duration of processing project usages:" + (System.currentTimeMillis() - before));
        }
    }

    public boolean isDeclarationsFound() {
        return myDeclarationsFound.get();
    }

    public void inspectionReadActionStarted() {
        myIsInProcess = true;
    }

    public void inspectionReadActionFinished() {
        myIsInProcess = false;
        if (myScope != null) {
            myScope.invalidate();
        }

        myCachedSortedRefs = null;
    }

    public void startOfflineView() {
        myOfflineView = true;
    }

    public boolean isOfflineView() {
        return myOfflineView;
    }

    public boolean isInProcess() {
        return myIsInProcess;
    }

    @Nonnull
    @Override
    public Project getProject() {
        return myProject;
    }

    @Nonnull
    @Override
    public RefProject getRefProject() {
        return myRefProject;
    }

    @Nonnull
    public List<RefElement> getSortedElements() {
        List<RefElement> answer = myCachedSortedRefs;
        if (answer != null) {
            return answer;
        }

        answer = new ArrayList<>(myRefTable.values());
        List<RefElement> list = answer;
        ReadAction.run(() -> Lists.quickSort(list, (o1, o2) -> {
            VirtualFile v1 = ((RefElementImpl)o1).getVirtualFile();
            VirtualFile v2 = ((RefElementImpl)o2).getVirtualFile();
            return (v1 != null ? v1.hashCode() : 0) - (v2 != null ? v2.hashCode() : 0);
        }));
        myCachedSortedRefs = answer = Collections.unmodifiableList(answer);
        return answer;
    }

    @Nonnull
    @Override
    public PsiManager getPsiManager() {
        return myPsiManager;
    }

    @Override
    public synchronized boolean isInGraph(VirtualFile file) {
        return !myUnprocessedFiles.get(((VirtualFileWithId)file).getId());
    }

    @Nullable
    @Override
    public PsiNamedElement getContainerElement(@Nonnull PsiElement element) {
        Language language = element.getLanguage();
        RefManagerExtension extension = myLanguageExtensions.get(language);
        if (extension == null) {
            return null;
        }
        return extension.getElementContainer(element);
    }

    private synchronized void registerUnprocessed(VirtualFileWithId virtualFile) {
        myUnprocessedFiles.set(virtualFile.getId());
    }

    public void removeReference(@Nonnull RefElement refElem) {
        final PsiElement element = refElem.getPsiElement();
        final RefManagerExtension extension = element != null ? getExtension(element.getLanguage()) : null;
        if (extension != null) {
            extension.removeReference(refElem);
        }

        if (element != null && myRefTable.remove(createAnchor(element)) != null) {
            return;
        }

        //PsiElement may have been invalidated and new one returned by getElement() is different so we need to do this stuff.
        for (Map.Entry<PsiAnchor, RefElement> entry : myRefTable.entrySet()) {
            RefElement value = entry.getValue();
            PsiAnchor anchor = entry.getKey();
            if (value == refElem) {
                myRefTable.remove(anchor);
                break;
            }
        }
        myCachedSortedRefs = null;
    }

    @Nonnull
    private static PsiAnchor createAnchor(@Nonnull final PsiElement element) {
        return ReadAction.compute(() -> PsiAnchor.create(element));
    }

    public void initializeAnnotators() {
        for (RefGraphAnnotator annotator : RefGraphAnnotator.EP_NAME.getExtensionList()) {
            registerGraphAnnotator(annotator);
        }
    }

    private class ProjectIterator extends PsiElementVisitor {
        @Override
        public void visitElement(PsiElement element) {
            ProgressManager.checkCanceled();
            final RefManagerExtension extension = getExtension(element.getLanguage());
            if (extension != null) {
                extension.visitElement(element);
            }
            else if (processExternalElements) {
                PsiFile file = element.getContainingFile();
                if (file != null) {
                    RefManagerExtension externalFileManagerExtension =
                        myExtensions.values().stream().filter(ex -> ex.shouldProcessExternalFile(file)).findFirst().orElse(null);
                    if (externalFileManagerExtension == null) {
                        if (element instanceof PsiFile) {
                            VirtualFile virtualFile = PsiUtilCore.getVirtualFile(element);
                            if (virtualFile instanceof VirtualFileWithId) {
                                registerUnprocessed((VirtualFileWithId)virtualFile);
                            }
                        }
                    }
                    else {
                        RefElement refFile = getReference(file);
                        LOG.assertTrue(refFile != null, file);
                        for (PsiReference reference : element.getReferences()) {
                            PsiElement resolve = reference.resolve();
                            if (resolve != null) {
                                fireNodeMarkedReferenced(resolve, file);
                                RefElement refWhat = getReference(resolve);
                                if (refWhat == null) {
                                    PsiFile targetContainingFile = resolve.getContainingFile();
                                    //no logic to distinguish different elements in the file anyway
                                    if (file == targetContainingFile) {
                                        continue;
                                    }
                                    refWhat = getReference(targetContainingFile);
                                }

                                if (refWhat != null) {
                                    ((RefElementImpl)refWhat).addInReference(refFile);
                                    ((RefElementImpl)refFile).addOutReference(refWhat);
                                }
                            }
                        }

                        Stream<? extends PsiElement> implicitRefs =
                            externalFileManagerExtension.extractExternalFileImplicitReferences(file);
                        implicitRefs.forEach(e -> {
                            RefElement superClassReference = getReference(e);
                            if (superClassReference != null) {
                                //in case of implicit inheritance, e.g. GroovyObject
                                //= no explicit reference is provided, dependency on groovy library could be treated as redundant though it is not
                                //inReference is not important in this case
                                ((RefElementImpl)refFile).addOutReference(superClassReference);
                            }
                        });

                        if (element instanceof PsiFile) {
                            externalFileManagerExtension.markExternalReferencesProcessed(refFile);
                        }
                    }
                }
            }
            for (PsiElement aChildren : element.getChildren()) {
                aChildren.accept(this);
            }
        }

        @Override
        public void visitFile(PsiFile file) {
            final VirtualFile virtualFile = file.getVirtualFile();
            if (virtualFile != null) {
                String relative =
                    ProjectUtilCore.displayUrlRelativeToProject(virtualFile, virtualFile.getPresentableUrl(), myProject, true, false);
                myContext.incrementJobDoneAmount(myContext.getStdJobDescriptors().BUILD_GRAPH, relative);
            }
            final FileViewProvider viewProvider = file.getViewProvider();
            final Set<Language> relevantLanguages = viewProvider.getLanguages();
            for (Language language : relevantLanguages) {
                try {
                    visitElement(viewProvider.getPsi(language));
                }
                catch (ProcessCanceledException | IndexNotReadyException e) {
                    throw e;
                }
                catch (Throwable e) {
                    LOG.error(new RuntimeExceptionWithAttachments(e, AttachmentFactory.get().create("diagnostics.txt", file.getName())));
                }
            }
            myPsiManager.dropResolveCaches();
            InjectedLanguageManager.getInstance(myProject).dropFileCaches(file);
        }
    }

    @Override
    @Nullable
    public RefElement getReference(@Nullable PsiElement elem) {
        return getReference(elem, false);
    }

    @Nullable
    public RefElement getReference(PsiElement elem, final boolean ignoreScope) {
        if (ReadAction.compute(() -> elem == null || !elem.isValid() || elem instanceof LightweightPsiElement
            || !(elem instanceof PsiDirectory) && !belongsToScope(elem, ignoreScope))) {
            return null;
        }

        return getFromRefTableOrCache(elem, () -> ReadAction.compute(() -> {
            final RefManagerExtension extension = getExtension(elem.getLanguage());
            if (extension != null) {
                final RefElement refElement = extension.createRefElement(elem);
                if (refElement != null) {
                    return (RefElementImpl)refElement;
                }
            }
            if (elem instanceof PsiFile) {
                return new RefFileImpl((PsiFile)elem, this);
            }
            if (elem instanceof PsiDirectory) {
                return new RefDirectoryImpl((PsiDirectory)elem, this);
            }
            return null;
        }), element -> ReadAction.run(() -> {
            element.initialize();
            for (RefManagerExtension each : myExtensions.values()) {
                each.onEntityInitialized(element, elem);
            }
            fireNodeInitialized(element);
        }));
    }

    private RefManagerExtension getExtension(final Language language) {
        return myLanguageExtensions.get(language);
    }

    @Nullable
    @Override
    public RefEntity getReference(final String type, final String fqName) {
        for (RefManagerExtension extension : myExtensions.values()) {
            final RefEntity refEntity = extension.getReference(type, fqName);
            if (refEntity != null) {
                return refEntity;
            }
        }
        if (SmartRefElementPointer.FILE.equals(type)) {
            return RefFileImpl.fileFromExternalName(this, fqName);
        }
        if (SmartRefElementPointer.MODULE.equals(type)) {
            return RefModuleImpl.moduleFromName(this, fqName);
        }
        if (SmartRefElementPointer.PROJECT.equals(type)) {
            return getRefProject();
        }
        if (SmartRefElementPointer.DIR.equals(type)) {
            String url = VirtualFileUtil.pathToUrl(ProjectPathMacroManager.getInstance(getProject()).expandPath(fqName));
            VirtualFile vFile = VirtualFileManager.getInstance().findFileByUrl(url);
            if (vFile != null) {
                final PsiDirectory dir = PsiManager.getInstance(getProject()).findDirectory(vFile);
                return getReference(dir);
            }
        }
        return null;
    }

    @Nullable
    public <T extends RefElement> T getFromRefTableOrCache(final PsiElement element, @Nonnull Supplier<? extends T> factory) {
        return getFromRefTableOrCache(element, factory, null);
    }

    @Nullable
    private <T extends RefElement> T getFromRefTableOrCache(
        @Nonnull PsiElement element,
        @Nonnull Supplier<? extends T> factory,
        @Nullable Consumer<? super T> whenCached
    ) {

        PsiAnchor psiAnchor = createAnchor(element);
        //noinspection unchecked
        T result = (T)(myRefTable.get(psiAnchor));

        if (result != null) {
            return result;
        }

        if (!isValidPointForReference()) {
            //LOG.assertTrue(true, "References may become invalid after process is finished");
            return null;
        }

        result = factory.get();
        if (result == null) {
            return null;
        }

        myCachedSortedRefs = null;
        RefElement prev = myRefTable.putIfAbsent(psiAnchor, result);
        if (prev != null) {
            //noinspection unchecked
            result = (T)prev;
        }
        else if (whenCached != null) {
            whenCached.accept(result);
        }

        return result;
    }

    @Override
    public RefModule getRefModule(@Nullable Module module) {
        if (module == null) {
            return null;
        }
        RefModule refModule = myModules.get(module);
        if (refModule == null) {
            refModule = Maps.cacheOrGet(myModules, module, new RefModuleImpl(module, this));
        }
        return refModule;
    }

    @Override
    public boolean belongsToScope(final PsiElement psiElement) {
        return belongsToScope(psiElement, false);
    }

    private boolean belongsToScope(final PsiElement psiElement, final boolean ignoreScope) {
        if (psiElement == null || !psiElement.isValid()) {
            return false;
        }
        if (psiElement instanceof PsiCompiledElement) {
            return false;
        }
        final PsiFile containingFile = ReadAction.compute(psiElement::getContainingFile);
        if (containingFile == null) {
            return false;
        }
        for (RefManagerExtension extension : myExtensions.values()) {
            if (!extension.belongsToScope(psiElement)) {
                return false;
            }
        }
        final Boolean inProject = ReadAction.compute(() -> psiElement.getManager().isInProject(psiElement));
        return inProject.booleanValue() && (ignoreScope || getScope() == null || getScope().contains(psiElement));
    }

    @Override
    public String getQualifiedName(RefEntity refEntity) {
        if (refEntity == null || refEntity instanceof RefElementImpl && !refEntity.isValid()) {
            return InspectionsBundle.message("inspection.reference.invalid");
        }

        return refEntity.getQualifiedName();
    }

    @Override
    public void removeRefElement(@Nonnull RefElement refElement, @Nonnull List<RefElement> deletedRefs) {
        List<RefEntity> children = refElement.getChildren();
        RefElement[] refElements = children.toArray(new RefElement[0]);
        for (RefElement refChild : refElements) {
            removeRefElement(refChild, deletedRefs);
        }

        ((RefManagerImpl)refElement.getRefManager()).removeReference(refElement);
        ((RefElementImpl)refElement).referenceRemoved();
        if (!deletedRefs.contains(refElement)) {
            deletedRefs.add(refElement);
        }
        else {
            LOG.error("deleted second time");
        }
    }

    public boolean isValidPointForReference() {
        return myIsInProcess || myOfflineView || ApplicationManager.getApplication().isUnitTestMode();
    }
}
