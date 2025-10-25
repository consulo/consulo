// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.language.editor.impl.inspection.reference;

import consulo.annotation.access.RequiredReadAction;
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
import consulo.language.editor.inspection.localize.InspectionLocalize;
import consulo.language.editor.inspection.reference.*;
import consulo.language.editor.internal.RefManagerInternal;
import consulo.language.editor.scope.AnalysisScope;
import consulo.language.file.FileViewProvider;
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
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jdom.Element;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class RefManagerImpl implements RefManagerInternal {
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
    private final Interner<String> myNameInterner = Interner.createStringInterner();

    @RequiredReadAction
    public RefManagerImpl(@Nonnull Project project, @Nullable AnalysisScope scope, @Nonnull GlobalInspectionContext context) {
        myProject = project;
        myScope = scope;
        myContext = context;
        myPsiManager = PsiManager.getInstance(project);
        myRefProject = new RefProjectImpl(this);
        project.getApplication().getExtensionPoint(InspectionExtensionsFactory.class).forEach(factory -> {
            RefManagerExtension<?> extension = factory.createRefManagerExtension(this);
            if (extension != null) {
                myExtensions.put(extension.getID(), extension);
                for (Language language : extension.getLanguages()) {
                    myLanguageExtensions.put(language, extension);
                }
            }
        });
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
        boolean forReading,
        boolean forWriting
    ) {
        for (RefGraphAnnotator annotator : myGraphAnnotators) {
            annotator.onMarkReferenced(refWhat, refFrom, referencedFromClassInitializer, forReading, forWriting);
        }
    }

    public void fireNodeMarkedReferenced(
        RefElement refWhat,
        RefElement refFrom,
        boolean referencedFromClassInitializer,
        boolean forReading,
        boolean forWriting,
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

    @Override
    public void registerGraphAnnotator(@Nonnull RefGraphAnnotator annotator) {
        if (myGraphAnnotators.add(annotator) && annotator instanceof RefGraphAnnotatorEx refGraphAnnotatorEx) {
            refGraphAnnotatorEx.initialize(this);
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
    public <T> T getExtension(@Nonnull Key<T> key) {
        //noinspection unchecked
        return (T) myExtensions.get(key);
    }

    @Override
    @Nullable
    public String getType(@Nonnull RefEntity ref) {
        for (RefManagerExtension extension : myExtensions.values()) {
            String type = extension.getType(ref);
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
    @RequiredReadAction
    public Element export(@Nonnull RefEntity refEntity, int actualLine) {
        refEntity = getRefinedElement(refEntity);

        Element problem = new Element("problem");

        if (refEntity instanceof RefDirectory refDirectory) {
            Element fileElement = new Element("file");
            VirtualFile virtualFile = ((PsiDirectory) refDirectory.getPsiElement()).getVirtualFile();
            fileElement.addContent(virtualFile.getUrl());
            problem.addContent(fileElement);
        }
        else if (refEntity instanceof RefElement refElement) {
            SmartPsiElementPointer pointer = refElement.getPointer();
            PsiFile psiFile = pointer.getContainingFile();
            if (psiFile == null) {
                return null;
            }

            Element fileElement = new Element("file");
            Element lineElement = new Element("line");
            VirtualFile virtualFile = psiFile.getVirtualFile();
            LOG.assertTrue(virtualFile != null);
            fileElement.addContent(virtualFile.getUrl());

            if (actualLine == -1) {
                Document document = PsiDocumentManager.getInstance(pointer.getProject()).getDocument(psiFile);
                LOG.assertTrue(document != null);
                Segment range = pointer.getRange();
                lineElement.addContent(String.valueOf(range != null ? document.getLineNumber(range.getStartOffset()) + 1 : -1));
            }
            else {
                lineElement.addContent(String.valueOf(actualLine + 1));
            }

            problem.addContent(fileElement);
            problem.addContent(lineElement);

            appendModule(problem, refElement.getModule());
        }
        else if (refEntity instanceof RefModule refModule) {
            VirtualFile moduleDir = refModule.getModule().getModuleDir();
            Element fileElement = new Element("file");
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
    public String getGroupName(@Nonnull RefElement entity) {
        for (RefManagerExtension extension : myExtensions.values()) {
            String groupName = extension.getGroupName(entity);
            if (groupName != null) {
                return groupName;
            }
        }

        RefEntity parent = entity.getOwner();
        while (parent != null && !(parent instanceof RefDirectory)) {
            parent = parent.getOwner();
        }
        LinkedList<String> containingDirs = new LinkedList<>();
        while (parent instanceof RefDirectory) {
            containingDirs.addFirst(parent.getName());
            parent = parent.getOwner();
        }
        return containingDirs.isEmpty() ? null : StringUtil.join(containingDirs, File.separator);
    }

    private static void appendModule(Element problem, RefModule refModule) {
        if (refModule != null) {
            Element moduleElement = new Element("module");
            moduleElement.addContent(refModule.getName());
            problem.addContent(moduleElement);
        }
    }

    public void findAllDeclarations() {
        if (!myDeclarationsFound.getAndSet(true)) {
            long before = System.currentTimeMillis();
            AnalysisScope scope = getScope();
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
        ReadAction.run(() -> Lists.quickSort(
            list,
            (o1, o2) -> {
                VirtualFile v1 = ((RefElementImpl) o1).getVirtualFile();
                VirtualFile v2 = ((RefElementImpl) o2).getVirtualFile();
                return (v1 != null ? v1.hashCode() : 0) - (v2 != null ? v2.hashCode() : 0);
            }
        ));
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
        return !myUnprocessedFiles.get(((VirtualFileWithId) file).getId());
    }

    @Nullable
    @Override
    @RequiredReadAction
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

    @RequiredReadAction
    public void removeReference(@Nonnull RefElement refElem) {
        PsiElement element = refElem.getPsiElement();
        RefManagerExtension extension = element != null ? getExtension(element.getLanguage()) : null;
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
    private static PsiAnchor createAnchor(@Nonnull PsiElement element) {
        return ReadAction.compute(() -> PsiAnchor.create(element));
    }

    public void initializeAnnotators() {
        getProject().getApplication().getExtensionPoint(RefGraphAnnotator.class)
            .forEach(this::registerGraphAnnotator);
    }

    private class ProjectIterator extends PsiElementVisitor {
        @Override
        @RequiredReadAction
        public void visitElement(PsiElement element) {
            ProgressManager.checkCanceled();
            RefManagerExtension extension = getExtension(element.getLanguage());
            if (extension != null) {
                extension.visitElement(element);
            }
            else if (processExternalElements) {
                PsiFile file = element.getContainingFile();
                if (file != null) {
                    RefManagerExtension externalFileManagerExtension =
                        myExtensions.values().stream().filter(ex -> ex.shouldProcessExternalFile(file)).findFirst().orElse(null);
                    if (externalFileManagerExtension == null) {
                        if (element instanceof PsiFile psiFile
                            && PsiUtilCore.getVirtualFile(psiFile) instanceof VirtualFileWithId virtualFileWithId) {
                            registerUnprocessed(virtualFileWithId);
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

                                if (refWhat instanceof RefElementImpl refElement) {
                                    refElement.addInReference(refFile);
                                    ((RefElementImpl) refFile).addOutReference(refElement);
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
                                ((RefElementImpl) refFile).addOutReference(superClassReference);
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
        @RequiredReadAction
        public void visitFile(PsiFile file) {
            VirtualFile virtualFile = file.getVirtualFile();
            if (virtualFile != null) {
                String relative =
                    ProjectUtilCore.displayUrlRelativeToProject(virtualFile, virtualFile.getPresentableUrl(), myProject, true, false);
                myContext.incrementJobDoneAmount(myContext.getStdJobDescriptors().BUILD_GRAPH, relative);
            }
            FileViewProvider viewProvider = file.getViewProvider();
            Set<Language> relevantLanguages = viewProvider.getLanguages();
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
    public RefElement getReference(PsiElement elem, boolean ignoreScope) {
        if (ReadAction.compute(() -> elem == null || !elem.isValid() || elem instanceof LightweightPsiElement
            || !(elem instanceof PsiDirectory) && !belongsToScope(elem, ignoreScope))) {
            return null;
        }

        return getFromRefTableOrCache(
            elem,
            () -> ReadAction.compute(() -> {
                RefManagerExtension extension = getExtension(elem.getLanguage());
                if (extension != null) {
                    RefElement refElement = extension.createRefElement(elem);
                    if (refElement != null) {
                        return refElement;
                    }
                }
                if (elem instanceof PsiFile file) {
                    return new RefFileImpl(file, this);
                }
                if (elem instanceof PsiDirectory directory) {
                    return new RefDirectoryImpl(directory, this);
                }
                return null;
            }),
            element -> ReadAction.run(() -> {
                ((RefElementImpl) element).initialize();
                for (RefManagerExtension each : myExtensions.values()) {
                    each.onEntityInitialized(element, elem);
                }
                fireNodeInitialized(element);
            })
        );
    }

    private RefManagerExtension getExtension(Language language) {
        return myLanguageExtensions.get(language);
    }

    @Nullable
    @Override
    @RequiredReadAction
    public RefEntity getReference(String type, String fqName) {
        for (RefManagerExtension extension : myExtensions.values()) {
            RefEntity refEntity = extension.getReference(type, fqName);
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
                PsiDirectory dir = PsiManager.getInstance(getProject()).findDirectory(vFile);
                return getReference(dir);
            }
        }
        return null;
    }

    @Nullable
    public <T extends RefElement> T getFromRefTableOrCache(PsiElement element, @Nonnull Supplier<? extends T> factory) {
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
        T result = (T) (myRefTable.get(psiAnchor));

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
            result = (T) prev;
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
    @RequiredReadAction
    public boolean belongsToScope(PsiElement psiElement) {
        return belongsToScope(psiElement, false);
    }

    @RequiredReadAction
    private boolean belongsToScope(PsiElement psiElement, boolean ignoreScope) {
        if (psiElement == null || !psiElement.isValid()) {
            return false;
        }
        if (psiElement instanceof PsiCompiledElement) {
            return false;
        }
        PsiFile containingFile = ReadAction.compute(psiElement::getContainingFile);
        if (containingFile == null) {
            return false;
        }
        for (RefManagerExtension extension : myExtensions.values()) {
            if (!extension.belongsToScope(psiElement)) {
                return false;
            }
        }
        Boolean inProject = ReadAction.compute(() -> psiElement.getManager().isInProject(psiElement));
        return inProject && (ignoreScope || getScope() == null || getScope().contains(psiElement));
    }

    @Override
    public String getQualifiedName(RefEntity refEntity) {
        if (refEntity == null || refEntity instanceof RefElementImpl && !refEntity.isValid()) {
            return InspectionLocalize.inspectionReferenceInvalid().get();
        }

        return refEntity.getQualifiedName();
    }

    @Override
    @RequiredReadAction
    public void removeRefElement(@Nonnull RefElement refElement, @Nonnull List<RefElement> deletedRefs) {
        List<RefEntity> children = refElement.getChildren();
        RefElement[] refElements = children.toArray(new RefElement[0]);
        for (RefElement refChild : refElements) {
            removeRefElement(refChild, deletedRefs);
        }

        ((RefManagerImpl) refElement.getRefManager()).removeReference(refElement);
        ((RefElementImpl) refElement).referenceRemoved();
        if (!deletedRefs.contains(refElement)) {
            deletedRefs.add(refElement);
        }
        else {
            LOG.error("deleted second time");
        }
    }

    public boolean isValidPointForReference() {
        return myIsInProcess || myOfflineView || getProject().getApplication().isUnitTestMode();
    }
}
