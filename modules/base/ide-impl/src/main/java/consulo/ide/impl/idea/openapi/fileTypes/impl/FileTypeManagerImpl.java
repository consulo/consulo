// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.openapi.fileTypes.impl;

import com.google.common.annotations.VisibleForTesting;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.application.ApplicationPropertiesComponent;
import consulo.application.impl.internal.ModalityStateImpl;
import consulo.application.impl.internal.concurent.BoundedTaskExecutor;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.component.messagebus.MessageBus;
import consulo.component.messagebus.MessageBusConnection;
import consulo.component.persist.*;
import consulo.component.persist.scheme.BaseSchemeProcessor;
import consulo.component.persist.scheme.SchemeManager;
import consulo.component.persist.scheme.SchemeManagerFactory;
import consulo.disposer.Disposable;
import consulo.document.util.FileContentUtilCore;
import consulo.ide.impl.idea.ide.util.PropertiesComponent;
import consulo.ide.impl.idea.openapi.fileTypes.UserBinaryFileType;
import consulo.ide.impl.idea.openapi.fileTypes.UserFileType;
import consulo.ide.impl.idea.openapi.fileTypes.ex.ExternalizableFileType;
import consulo.ide.impl.idea.openapi.fileTypes.ex.FileTypeChooser;
import consulo.language.internal.FileTypeManagerEx;
import consulo.language.Language;
import consulo.language.file.LanguageFileType;
import consulo.language.file.light.LightVirtualFile;
import consulo.language.internal.custom.SyntaxTable;
import consulo.language.plain.PlainTextFileType;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.internal.GuiUtils;
import consulo.util.collection.ArrayUtil;
import consulo.util.io.FileUtil;
import consulo.util.io.URLUtil;
import consulo.util.jdom.JDOMUtil;
import consulo.util.lang.*;
import consulo.util.xml.serializer.JDOMExternalizer;
import consulo.virtualFileSystem.*;
import consulo.virtualFileSystem.event.BulkFileListener;
import consulo.virtualFileSystem.event.VFileEvent;
import consulo.virtualFileSystem.fileType.*;
import consulo.virtualFileSystem.impl.internal.fileType.FileTypeDetectionService;
import consulo.virtualFileSystem.internal.FileTypeAssocTable;
import consulo.virtualFileSystem.internal.matcher.ExactFileNameMatcherImpl;
import consulo.virtualFileSystem.internal.matcher.ExtensionFileNameMatcherImpl;
import org.jspecify.annotations.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jdom.Element;
import org.jetbrains.annotations.TestOnly;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

@Singleton
@ServiceImpl
@State(name = "FileTypeManager", storages = @Storage("filetypes.xml"), additionalExportFile = FileTypeManagerImpl.FILE_SPEC)
public class FileTypeManagerImpl extends FileTypeManagerEx implements PersistentStateComponent<Element>, Disposable {
    private static final Logger LOG = Logger.getInstance(FileTypeManagerImpl.class);

    // You must update all existing default configurations accordingly
    private static final int VERSION = 17;
    private static final ThreadLocal<Pair<VirtualFile, FileType>> FILE_TYPE_FIXED_TEMPORARILY = new ThreadLocal<>();

    // must be sorted
    @SuppressWarnings("SpellCheckingInspection")
    static final String DEFAULT_IGNORED =
        "*.hprof;*.pyc;*.pyo;*.rbc;*.yarb;*~;.DS_Store;.git;.hg;.svn;CVS;__pycache__;_svn;vssver.scc;vssver2.scc;";

    private final Set<FileType> myDefaultTypes = new HashSet<>();
    private FileTypeIdentifiableByVirtualFile[] mySpecialFileTypes = FileTypeIdentifiableByVirtualFile.EMPTY_ARRAY;

    private FileTypeAssocTable<FileType> myPatternsTable = new FileTypeAssocTable<>();
    private final IgnoredPatternSet myIgnoredPatterns = new IgnoredPatternSet();
    private final IgnoredFileCache myIgnoredFileCache = new IgnoredFileCache(myIgnoredPatterns);

    private final FileTypeAssocTable<FileType> myInitialAssociations = new FileTypeAssocTable<>();
    private final Map<FileNameMatcher, String> myUnresolvedMappings = new HashMap<>();
    private final RemovedMappingTracker myRemovedMappingTracker = new RemovedMappingTracker();

    private static final String ELEMENT_FILETYPE = "filetype";
    private static final String ELEMENT_IGNORE_FILES = "ignoreFiles";
    private static final String ATTRIBUTE_LIST = "list";

    private static final String ATTRIBUTE_VERSION = "version";
    private static final String ATTRIBUTE_NAME = "name";
    private static final String ATTRIBUTE_DESCRIPTION = "description";

    private static class StandardFileType {

        private final FileType fileType;

        private final List<FileNameMatcher> matchers;

        private StandardFileType(FileType fileType, List<FileNameMatcher> matchers) {
            this.fileType = fileType;
            this.matchers = matchers;
        }
    }

    private final MessageBus myMessageBus;
    private final Map<String, StandardFileType> myStandardFileTypes = new LinkedHashMap<>();
    private final SchemeManager<FileType, AbstractFileType> mySchemeManager;

    static final String FILE_SPEC = StoragePathMacros.ROOT_CONFIG + "/filetypes";

    private final Object PENDING_INIT_LOCK = new Object();

    private final FileTypeDetectionService myDetectionService;

    @Inject
    public FileTypeManagerImpl(
        Application application,
        SchemeManagerFactory schemeManagerFactory,
        ApplicationPropertiesComponent propertiesComponent
    ) {
        int fileTypeChangedCounter = propertiesComponent.getInt("fileTypeChangedCounter", 0);
        fileTypeChangedCount = new AtomicInteger(fileTypeChangedCounter);
        myDetectionService = new FileTypeDetectionService(application, fileTypeChangedCounter, this) {
            @Override
            protected FileType getDefaultTextFileType() {
                return PlainTextFileType.INSTANCE;
            }

            @Override
            protected @Nullable FileType getFileTypeByFileWithoutContent(VirtualFile file) {
                return getByFile(file);
            }

            @Override
            protected void onDetectedFileTypesChanged(List<VirtualFile> changed, List<VirtualFile> crashed) {
                if (!changed.isEmpty()) {
                    application.invokeLater(() -> FileContentUtilCore.reparseFiles(changed), application.getDisposed());
                }
                if (!crashed.isEmpty()) {
                    // do not re-scan locked or invalid files too often to avoid constant disk thrashing
                    // if that condition is permanent
                    AppExecutorUtil.getAppScheduledExecutorService()
                        .schedule(() -> FileContentUtilCore.reparseFiles(crashed), 10, TimeUnit.SECONDS);
                }
            }
        };

        myMessageBus = application.getMessageBus();
        mySchemeManager = schemeManagerFactory.createSchemeManager(FILE_SPEC, new BaseSchemeProcessor<FileType, AbstractFileType>() {
            @Override
            public AbstractFileType readScheme(Element element, boolean duringLoad) {
                if (!duringLoad) {
                    fireBeforeFileTypesChanged();
                }
                AbstractFileType type = (AbstractFileType)loadFileType(element, false);
                if (!duringLoad) {
                    fireFileTypesChanged(type, null);
                }
                return type;
            }

            @Override
            public State getState(AbstractFileType fileType) {
                if (!shouldSave(fileType)) {
                    return State.NON_PERSISTENT;
                }
                if (!myDefaultTypes.contains(fileType)) {
                    return State.POSSIBLY_CHANGED;
                }
                return fileType.isModified() ? State.POSSIBLY_CHANGED : State.NON_PERSISTENT;
            }

            @Override
            public Element writeScheme(AbstractFileType fileType) {
                Element root = new Element(ELEMENT_FILETYPE);

                root.setAttribute("binary", String.valueOf(fileType.isBinary()));
                if (!StringUtil.isEmpty(fileType.getDefaultExtension())) {
                    root.setAttribute("default_extension", fileType.getDefaultExtension());
                }
                root.setAttribute(ATTRIBUTE_DESCRIPTION, fileType.getDescription().get());
                root.setAttribute(ATTRIBUTE_NAME, fileType.getId());

                fileType.writeExternal(root);

                Element map = new Element(AbstractFileType.ELEMENT_EXTENSION_MAP);
                writeExtensionsMap(map, fileType, false);
                if (!map.getChildren().isEmpty()) {
                    root.addContent(map);
                }
                return root;
            }

            @Override
            public void onSchemeDeleted(AbstractFileType scheme) {
                GuiUtils.invokeLaterIfNeeded(
                    () -> {
                        Application app = Application.get();
                        app.runWriteAction(() -> fireBeforeFileTypesChanged());
                        myPatternsTable.removeAllAssociations(scheme);
                        app.runWriteAction(() -> fireFileTypesChanged(null, scheme));
                    },
                    ModalityStateImpl.NON_MODAL
                );
            }

            @Override
            public String getName(FileType immutableElement) {
                return immutableElement.getId();
            }
        }, RoamingType.DEFAULT);

        initStandardFileTypes();

        myMessageBus.connect().subscribe(BulkFileListener.class, new BulkFileListener() {
            @Override
            public void after(List<? extends VFileEvent> events) {
                myDetectionService.queueForReDetect(events);
            }
        });

        myIgnoredPatterns.setIgnoreMasks(DEFAULT_IGNORED);
    }

    @VisibleForTesting
    void initStandardFileTypes() {
        FileTypeConsumer consumer = new FileTypeConsumer() {
            @Override
            public void consume(FileType fileType) {
                register(fileType, parse(fileType.getDefaultExtension()));
            }

            @Override
            public void consume(FileType fileType, String extensions) {
                register(fileType, parse(extensions));
            }

            @Override
            public void consume(FileType fileType, FileNameMatcher... matchers) {
                register(fileType, new ArrayList<>(Arrays.asList(matchers)));
            }

            private void register(FileType fileType, List<FileNameMatcher> fileNameMatchers) {
                StandardFileType type = myStandardFileTypes.get(fileType.getId());
                if (type != null) {
                    type.matchers.addAll(fileNameMatchers);
                }
                else {
                    myStandardFileTypes.put(fileType.getId(), new StandardFileType(fileType, fileNameMatchers));
                }
            }
        };

        FileTypeFactory.FILE_TYPE_FACTORY_EP.forEachExtensionSafe(factory -> factory.createFileTypes(consumer));

        for (StandardFileType pair : myStandardFileTypes.values()) {
            registerFileTypeWithoutNotification(pair.fileType, pair.matchers, true);
        }

        try {
            URL defaultFileTypesUrl = FileTypeManagerImpl.class.getResource("/defaultFileTypes.xml");
            if (defaultFileTypesUrl != null) {
                Element defaultFileTypesElement = JDOMUtil.load(URLUtil.openStream(defaultFileTypesUrl));
                for (Element e : defaultFileTypesElement.getChildren()) {
                    if ("filetypes".equals(e.getName())) {
                        for (Element element : e.getChildren(ELEMENT_FILETYPE)) {
                            String fileTypeName = element.getAttributeValue(ATTRIBUTE_NAME);
                            loadFileType(element, true);
                        }
                    }
                    else if (AbstractFileType.ELEMENT_EXTENSION_MAP.equals(e.getName())) {
                        readGlobalMappings(e, true);
                    }
                }
            }
        }
        catch (Exception e) {
            LOG.error(e);
        }
    }

    @TestOnly
    boolean toLog;

    private boolean toLog() {
        return toLog;
    }

    private static void log(String message) {
        LOG.debug(message + " - " + Thread.currentThread());
    }

    public void drainReDetectQueue() {
        try {
            ((BoundedTaskExecutor)myDetectionService.getReDetectExecutor()).waitAllTasksExecuted(1, TimeUnit.MINUTES);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public FileType getStdFileType(String name) {
        StandardFileType stdFileType;
        synchronized (PENDING_INIT_LOCK) {
            stdFileType = myStandardFileTypes.get(name);
        }
        return stdFileType != null ? stdFileType.fileType : PlainTextFileType.INSTANCE;
    }

    @Override
    public void afterLoad(boolean first) {
        if (!myUnresolvedMappings.isEmpty()) {
            for (StandardFileType pair : myStandardFileTypes.values()) {
                registerReDetectedMappings(pair);
            }
        }

        // resolve unresolved mappings initialized before certain plugin initialized
        if (!myUnresolvedMappings.isEmpty()) {
            for (StandardFileType pair : myStandardFileTypes.values()) {
                bindUnresolvedMappings(pair.fileType);
            }
        }
    }

    @Override
    public FileType getFileTypeByFileName(String fileName) {
        return getFileTypeByFileName((CharSequence)fileName);
    }

    @Override
    public FileType getFileTypeByFileName(CharSequence fileName) {
        synchronized (PENDING_INIT_LOCK) {
            FileType type = myPatternsTable.findAssociatedFileType(fileName);
            return ObjectUtil.notNull(type, UnknownFileType.INSTANCE);
        }
    }

    @Override
    public void freezeFileTypeTemporarilyIn(VirtualFile file, Runnable runnable) {
        FileType fileType = file.getFileType();
        Pair<VirtualFile, FileType> old = FILE_TYPE_FIXED_TEMPORARILY.get();
        FILE_TYPE_FIXED_TEMPORARILY.set(Pair.create(file, fileType));
        if (toLog()) {
            log("F: freezeFileTypeTemporarilyIn(" + file.getName() + ") to " + fileType.getId() + " in " + Thread.currentThread());
        }
        try {
            runnable.run();
        }
        finally {
            if (old == null) {
                FILE_TYPE_FIXED_TEMPORARILY.remove();
            }
            else {
                FILE_TYPE_FIXED_TEMPORARILY.set(old);
            }
            if (toLog()) {
                log("F: unfreezeFileType(" + file.getName() + ") in " + Thread.currentThread());
            }
        }
    }

    @Override

    public FileType getFileTypeByFile(VirtualFile file) {
        return getFileTypeByFile(file, null);
    }

    @Override

    public FileType getFileTypeByFile(VirtualFile file, @Nullable byte[] content) {
        FileType overriddenFileType = FileTypeOverrider.EP_NAME.computeSafeIfAny((overrider) -> overrider.getOverriddenFileType(file));
        if (overriddenFileType != null) {
            return overriddenFileType;
        }

        FileType fileType = getByFile(file);
        if (!(file instanceof StubVirtualFile)) {
            if (fileType == null) {
                return myDetectionService.getOrDetectFromContent(file, content);
            }
            if (FileTypeDetectionService.mightBeReplacedByDetectedFileType(fileType)) {
                FileType detectedFromContent = myDetectionService.getOrDetectFromContent(file, content);
                if (detectedFromContent != UnknownFileType.INSTANCE && detectedFromContent != PlainTextFileType.INSTANCE) {
                    return detectedFromContent;
                }
            }
        }
        return ObjectUtil.notNull(fileType, UnknownFileType.INSTANCE);
    }

    @Nullable // null means all conventional detect methods returned UnknownFileType.INSTANCE, have to detect from content
    public FileType getByFile(VirtualFile file) {
        Pair<VirtualFile, FileType> fixedType = FILE_TYPE_FIXED_TEMPORARILY.get();
        if (fixedType != null && fixedType.getFirst().equals(file)) {
            FileType fileType = fixedType.getSecond();
            if (toLog()) {
                log("F: getByFile(" + file.getName() + ") was frozen to " + fileType.getId() + " in " + Thread.currentThread());
            }
            return fileType;
        }

        if (file instanceof LightVirtualFile lightVirtualFile) {
            FileType fileType = lightVirtualFile.getAssignedFileType();
            if (fileType != null) {
                return fileType;
            }
        }

        for (FileTypeIdentifiableByVirtualFile type : mySpecialFileTypes) {
            if (type.isMyFileType(file)) {
                if (toLog()) {
                    log("F: getByFile(" + file.getName() + "): Special file type: " + type.getId());
                }
                return type;
            }
        }

        FileType fileType = getFileTypeByFileName(file.getNameSequence());
        if (fileType == UnknownFileType.INSTANCE) {
            fileType = null;
        }
        if (toLog()) {
            log("F: getByFile(" + file.getName() + ") By name file type: " + (fileType == null ? null : fileType.getId()));
        }
        return fileType;
    }

    void clearCaches() {
        myDetectionService.clearCaches();
    }

    private void clearPersistentAttributes() {
        int count = fileTypeChangedCount.incrementAndGet();
        myDetectionService.setFileTypeChangedCounter(count);
        PropertiesComponent.getInstance().setValue("fileTypeChangedCounter", Integer.toString(count));
    }

    @Override
    public FileType findFileTypeByName(String fileTypeName) {
        FileType type = getStdFileType(fileTypeName);
        // TODO: Abstract file types are not std one, so need to be restored specially,
        // currently there are 6 of them and restoration does not happen very often so just iteration is enough
        if (type == PlainTextFileType.INSTANCE && !fileTypeName.equals(type.getId())) {
            for (FileType fileType : mySchemeManager.getAllSchemes()) {
                if (fileTypeName.equals(fileType.getId())) {
                    return fileType;
                }
            }
        }
        return type;
    }

    
    @Override
    public boolean isFileOfType(VirtualFile file, FileType type) {
        if (FileTypeDetectionService.mightBeReplacedByDetectedFileType(type) || type.equals(UnknownFileType.INSTANCE)) {
            // a file has unknown file type if none of file type detectors matched it
            // for plain text file type, we run file type detection based on content

            return file.getFileType().equals(type);
        }

        if (file instanceof LightVirtualFile lightVirtualFile) {
            FileType assignedFileType = lightVirtualFile.getAssignedFileType();
            if (assignedFileType != null) {
                return type.equals(assignedFileType);
            }
        }

        FileType overriddenFileType = FileTypeOverrider.EP_NAME.computeSafeIfAny((overrider) -> overrider.getOverriddenFileType(file));
        if (overriddenFileType != null) {
            return overriddenFileType.equals(type);
        }

        if (type instanceof FileTypeIdentifiableByVirtualFile fakeFileType && fakeFileType.isMyFileType(file)) {
            return true;
        }

        FileType fileTypeByFileName = getFileTypeByFileName(file.getNameSequence());
        if (fileTypeByFileName == type) {
            return true;
        }
        if (fileTypeByFileName != UnknownFileType.INSTANCE) {
            return false;
        }
        if (file instanceof StubVirtualFile) {
            return false;
        }

        return type.equals(myDetectionService.getOrDetectFromContent(file, null));
    }

    @Override
    public LanguageFileType findFileTypeByLanguage(Language language) {
        // Do not use getRegisteredFileTypes() to avoid instantiating all pending file types
        return language.findMyFileType(mySchemeManager.getAllSchemes().toArray(FileType.EMPTY_ARRAY));
    }

    @Override
    public FileType getFileTypeByExtension(String extension) {
        synchronized (PENDING_INIT_LOCK) {
            FileType type = myPatternsTable.findByExtension(extension);
            return ObjectUtil.notNull(type, UnknownFileType.INSTANCE);
        }
    }

    private void unregisterFileTypeWithoutNotification(FileType fileType) {
        myPatternsTable.removeAllAssociations(fileType);
        myInitialAssociations.removeAllAssociations(fileType);
        mySchemeManager.removeScheme(fileType);
        if (fileType instanceof FileTypeIdentifiableByVirtualFile fakeFileType) {
            mySpecialFileTypes = ArrayUtil.remove(
                mySpecialFileTypes,
                fakeFileType,
                FileTypeIdentifiableByVirtualFile.ARRAY_FACTORY
            );
        }
    }

    @Override

    public FileType[] getRegisteredFileTypes() {
        Collection<FileType> fileTypes = mySchemeManager.getAllSchemes();
        return fileTypes.toArray(FileType.EMPTY_ARRAY);
    }

    @Override

    public String getExtension(String fileName) {
        return FileUtil.getExtension(fileName);
    }

    @Override
    public Set<String> getIgnoredFiles() {
        return myIgnoredPatterns.getIgnoreMasks();
    }

    @Override
    public void setIgnoredFiles(Set<String> list) {
        fireBeforeFileTypesChanged();
        myIgnoredFileCache.clearCache();
        myIgnoredPatterns.setIgnoreMasks(list);
        fireFileTypesChanged();
    }

    @Override
    public boolean isFileIgnored(String name) {
        return myIgnoredPatterns.isIgnored(name);
    }

    @Override
    public boolean isFileIgnored(VirtualFile file) {
        return myIgnoredFileCache.isFileIgnored(file);
    }

    @Override

    public String[] getAssociatedExtensions(FileType type) {
        synchronized (PENDING_INIT_LOCK) {
            //noinspection deprecation
            return myPatternsTable.getAssociatedExtensions(type);
        }
    }

    @Override

    public List<FileNameMatcher> getAssociations(FileType type) {
        synchronized (PENDING_INIT_LOCK) {
            return myPatternsTable.getAssociations(type);
        }
    }

    @Override
    public void associate(FileType type, FileNameMatcher matcher) {
        associate(type, matcher, true);
    }

    @Override
    public void removeAssociation(FileType type, FileNameMatcher matcher) {
        removeAssociation(type, matcher, true);
    }

    @Override
    public void fireBeforeFileTypesChanged() {
        FileTypeEvent event = new FileTypeEvent(this, null, null);
        myMessageBus.syncPublisher(FileTypeListener.class).beforeFileTypesChanged(event);
    }

    private final AtomicInteger fileTypeChangedCount;

    @Override
    public void fireFileTypesChanged() {
        fireFileTypesChanged(null, null);
    }

    public void fireFileTypesChanged(@Nullable FileType addedFileType, @Nullable FileType removedFileType) {
        clearCaches();
        clearPersistentAttributes();
        myMessageBus.syncPublisher(FileTypeListener.class).fileTypesChanged(new FileTypeEvent(this, addedFileType, removedFileType));
    }

    private final Map<FileTypeListener, MessageBusConnection> myAdapters = new HashMap<>();

    @Override
    public void addFileTypeListener(FileTypeListener listener) {
        MessageBusConnection connection = myMessageBus.connect();
        connection.subscribe(FileTypeListener.class, listener);
        myAdapters.put(listener, connection);
    }

    @Override
    public void removeFileTypeListener(FileTypeListener listener) {
        MessageBusConnection connection = myAdapters.remove(listener);
        if (connection != null) {
            connection.disconnect();
        }
    }

    @Override
    public void loadState(Element state) {
        int savedVersion = StringUtil.parseInt(state.getAttributeValue(ATTRIBUTE_VERSION), 0);

        for (Element element : state.getChildren()) {
            if (element.getName().equals(ELEMENT_IGNORE_FILES)) {
                myIgnoredPatterns.setIgnoreMasks(element.getAttributeValue(ATTRIBUTE_LIST));
            }
            else if (AbstractFileType.ELEMENT_EXTENSION_MAP.equals(element.getName())) {
                readGlobalMappings(element, false);
            }
        }

        if (savedVersion < 4) {
            if (savedVersion == 0) {
                addIgnore(".svn");
            }

            addIgnore("*.pyc");
            addIgnore("*.pyo");
            addIgnore(".git");
        }

        if (savedVersion < 5) {
            addIgnore("*.hprof");
        }

        if (savedVersion < 6) {
            addIgnore("_svn");
        }

        if (savedVersion < 7) {
            addIgnore(".hg");
        }

        if (savedVersion < 8) {
            addIgnore("*~");
        }

        if (savedVersion < 9) {
            addIgnore("__pycache__");
        }

        if (savedVersion < 11) {
            addIgnore("*.rbc");
        }

        if (savedVersion < 13) {
            // we want *.lib back since it's an important user artifact for CLion, also for IDEA project itself, since we have some libs.
            unignoreMask("*.lib");
        }

        if (savedVersion < 15) {
            // we want .bundle back, bundler keeps useful data there
            unignoreMask(".bundle");
        }

        if (savedVersion < 16) {
            // we want .tox back to allow users selecting interpreters from it
            unignoreMask(".tox");
        }

        if (savedVersion < 17) {
            addIgnore("*.rbc");
        }

        myIgnoredFileCache.clearCache();

        String counter = JDOMExternalizer.readString(state, "fileTypeChangedCounter");
        if (counter != null) {
            fileTypeChangedCount.set(StringUtil.parseInt(counter, 0));
            myDetectionService.setFileTypeChangedCounter(fileTypeChangedCount.get());
        }
    }

    private void unignoreMask(String maskToRemove) {
        Set<String> masks = new LinkedHashSet<>(myIgnoredPatterns.getIgnoreMasks());
        masks.remove(maskToRemove);

        myIgnoredPatterns.clearPatterns();
        for (String each : masks) {
            myIgnoredPatterns.addIgnoreMask(each);
        }
    }

    private void readGlobalMappings(Element e, boolean isAddToInit) {
        for (Pair<FileNameMatcher, String> association : AbstractFileType.readAssociations(e)) {
            FileType type = getFileTypeByName(association.getSecond());
            FileNameMatcher matcher = association.getFirst();

            if (type != null) {
                if (PlainTextFileType.INSTANCE == type) {
                    FileType newFileType = myPatternsTable.findAssociatedFileType(matcher);
                    if (newFileType != null && newFileType != PlainTextFileType.INSTANCE && newFileType != UnknownFileType.INSTANCE) {
                        myRemovedMappingTracker.add(matcher, newFileType.getId(), false);
                    }
                }
                associate(type, matcher, false);
                if (isAddToInit) {
                    myInitialAssociations.addAssociation(matcher, type);
                }
            }
            else {
                myUnresolvedMappings.put(matcher, association.getSecond());
            }
        }

        myRemovedMappingTracker.load(e);
        for (RemovedMappingTracker.RemovedMapping mapping : myRemovedMappingTracker.getRemovedMappings()) {
            FileType fileType = getFileTypeByName(mapping.getFileTypeName());
            if (fileType != null) {
                removeAssociation(fileType, mapping.getFileNameMatcher(), false);
            }
        }
    }

    private void addIgnore(String ignoreMask) {
        myIgnoredPatterns.addIgnoreMask(ignoreMask);
    }

    @Override
    public Element getState() {
        Element state = new Element("state");

        Set<String> masks = myIgnoredPatterns.getIgnoreMasks();
        String ignoreFiles;
        if (masks.isEmpty()) {
            ignoreFiles = "";
        }
        else {
            String[] strings = ArrayUtil.toStringArray(masks);
            Arrays.sort(strings);
            ignoreFiles = StringUtil.join(strings, ";") + ";";
        }

        if (!ignoreFiles.equalsIgnoreCase(DEFAULT_IGNORED)) {
            // empty means empty list - we need to distinguish null and empty to apply or not to apply default value
            state.addContent(new Element(ELEMENT_IGNORE_FILES).setAttribute(ATTRIBUTE_LIST, ignoreFiles));
        }

        Element map = new Element(AbstractFileType.ELEMENT_EXTENSION_MAP);

        List<FileType> notExternalizableFileTypes = new ArrayList<>();
        for (FileType type : mySchemeManager.getAllSchemes()) {
            if (!(type instanceof AbstractFileType) || myDefaultTypes.contains(type)) {
                notExternalizableFileTypes.add(type);
            }
        }
        if (!notExternalizableFileTypes.isEmpty()) {
            Collections.sort(notExternalizableFileTypes, Comparator.comparing(FileType::getId));
            for (FileType type : notExternalizableFileTypes) {
                writeExtensionsMap(map, type, true);
            }
        }

        // https://youtrack.jetbrains.com/issue/IDEA-138366
        myRemovedMappingTracker.save(map);

        if (!myUnresolvedMappings.isEmpty()) {
            FileNameMatcher[] unresolvedMappingKeys = myUnresolvedMappings.keySet().toArray(new FileNameMatcher[0]);
            Arrays.sort(unresolvedMappingKeys, Comparator.comparing(FileNameMatcher::getPresentableString));

            for (FileNameMatcher fileNameMatcher : unresolvedMappingKeys) {
                Element content = AbstractFileType.writeMapping(myUnresolvedMappings.get(fileNameMatcher), fileNameMatcher, true);
                if (content != null) {
                    map.addContent(content);
                }
            }
        }

        if (!map.getChildren().isEmpty()) {
            state.addContent(map);
        }

        if (!state.getChildren().isEmpty()) {
            state.setAttribute(ATTRIBUTE_VERSION, String.valueOf(VERSION));
        }
        return state;
    }

    private void writeExtensionsMap(Element map, FileType type, boolean specifyTypeName) {
        List<FileNameMatcher> associations = myPatternsTable.getAssociations(type);
        Set<FileNameMatcher> defaultAssociations = new HashSet<>(myInitialAssociations.getAssociations(type));

        for (FileNameMatcher matcher : associations) {
            boolean isDefaultAssociationContains = defaultAssociations.remove(matcher);
            if (!isDefaultAssociationContains && shouldSave(type)) {
                Element content = AbstractFileType.writeMapping(type.getId(), matcher, specifyTypeName);
                if (content != null) {
                    map.addContent(content);
                }
            }
        }

        myRemovedMappingTracker.saveRemovedMappingsForFileType(map, type.getId(), defaultAssociations, specifyTypeName);
    }

    // -------------------------------------------------------------------------
    // Helper methods
    // -------------------------------------------------------------------------

    private @Nullable FileType getFileTypeByName(String name) {
        synchronized (PENDING_INIT_LOCK) {
            return mySchemeManager.findSchemeByName(name);
        }
    }

    private static List<FileNameMatcher> parse(@Nullable String semicolonDelimited) {
        return parse(semicolonDelimited, ExtensionFileNameMatcherImpl::new);
    }

    private static List<FileNameMatcher> parse(
        @Nullable String semicolonDelimited,
        Function<? super String, ? extends FileNameMatcher> matcherFactory
    ) {
        if (semicolonDelimited == null) {
            return Collections.emptyList();
        }

        StringTokenizer tokenizer = new StringTokenizer(semicolonDelimited, FileTypeConsumer.EXTENSION_DELIMITER, false);
        ArrayList<FileNameMatcher> list = new ArrayList<>(semicolonDelimited.length() / "py;".length());
        while (tokenizer.hasMoreTokens()) {
            list.add(matcherFactory.apply(tokenizer.nextToken().trim()));
        }
        return list;
    }

    /**
     * Registers a standard file type. Doesn't notifyListeners any change events.
     */
    private void registerFileTypeWithoutNotification(FileType fileType, List<? extends FileNameMatcher> matchers, boolean addScheme) {
        if (addScheme) {
            mySchemeManager.addNewScheme(fileType, true);
        }
        for (FileNameMatcher matcher : matchers) {
            myPatternsTable.addAssociation(matcher, fileType);
            myInitialAssociations.addAssociation(matcher, fileType);
        }

        if (fileType instanceof FileTypeIdentifiableByVirtualFile fileTypeIdentifiableByVirtualFile) {
            mySpecialFileTypes = ArrayUtil.append(
                mySpecialFileTypes,
                fileTypeIdentifiableByVirtualFile,
                FileTypeIdentifiableByVirtualFile.ARRAY_FACTORY
            );
        }
    }

    private void bindUnresolvedMappings(FileType fileType) {
        for (FileNameMatcher matcher : new HashSet<>(myUnresolvedMappings.keySet())) {
            String name = myUnresolvedMappings.get(matcher);
            if (Comparing.equal(name, fileType.getId())) {
                myPatternsTable.addAssociation(matcher, fileType);
                myUnresolvedMappings.remove(matcher);
            }
        }

        for (FileNameMatcher matcher : myRemovedMappingTracker.getMappingsForFileType(fileType.getId())) {
            removeAssociation(fileType, matcher, false);
        }
    }

    private FileType loadFileType(Element typeElement, boolean isDefault) {
        String fileTypeName = typeElement.getAttributeValue(ATTRIBUTE_NAME);
        String fileTypeDescr = typeElement.getAttributeValue(ATTRIBUTE_DESCRIPTION);
        String iconPath = typeElement.getAttributeValue("icon");

        String extensionsStr = StringUtil.nullize(typeElement.getAttributeValue("extensions"));
        if (isDefault && extensionsStr != null) {
            // todo support wildcards
            extensionsStr = filterAlreadyRegisteredExtensions(extensionsStr);
        }

        FileType type = isDefault ? getFileTypeByName(fileTypeName) : null;
        if (type != null) {
            return type;
        }

        Element element = typeElement.getChild(AbstractFileType.ELEMENT_HIGHLIGHTING);
        if (element == null) {
            type = new UserBinaryFileType();
        }
        else {
            SyntaxTable table = AbstractFileType.readSyntaxTable(element);
            type = new AbstractFileType(table);
            ((AbstractFileType)type).initSupport();
        }

        setFileTypeAttributes((UserFileType)type, fileTypeName, fileTypeDescr, iconPath);
        registerFileTypeWithoutNotification(type, parse(extensionsStr), isDefault);

        if (isDefault) {
            myDefaultTypes.add(type);
            if (type instanceof ExternalizableFileType externalizableFileType) {
                externalizableFileType.markDefaultSettings();
            }
        }
        else {
            Element extensions = typeElement.getChild(AbstractFileType.ELEMENT_EXTENSION_MAP);
            if (extensions != null) {
                for (Pair<FileNameMatcher, String> association : AbstractFileType.readAssociations(extensions)) {
                    associate(type, association.getFirst(), false);
                }

                for (RemovedMappingTracker.RemovedMapping removedAssociation : RemovedMappingTracker.readRemovedMappings(extensions)) {
                    removeAssociation(type, removedAssociation.getFileNameMatcher(), false);
                }
            }
        }
        return type;
    }

    private @Nullable String filterAlreadyRegisteredExtensions(String semicolonDelimited) {
        StringTokenizer tokenizer = new StringTokenizer(semicolonDelimited, FileTypeConsumer.EXTENSION_DELIMITER, false);
        StringBuilder builder = null;
        while (tokenizer.hasMoreTokens()) {
            String extension = tokenizer.nextToken().trim();
            if (getFileTypeByExtension(extension) == UnknownFileType.INSTANCE) {
                if (builder == null) {
                    builder = new StringBuilder();
                }
                else if (builder.length() > 0) {
                    builder.append(FileTypeConsumer.EXTENSION_DELIMITER);
                }
                builder.append(extension);
            }
        }
        return builder == null ? null : builder.toString();
    }

    private static void setFileTypeAttributes(
        UserFileType fileType,
        @Nullable String name,
        @Nullable String description,
        @Nullable String iconPath
    ) {
        if (!StringUtil.isEmptyOrSpaces(iconPath)) {
            fileType.setIconPath(iconPath);
        }
        if (description != null) {
            fileType.setDescription(description);
        }
        if (name != null) {
            fileType.setName(name);
        }
    }

    private static boolean shouldSave(FileType fileType) {
        return fileType != UnknownFileType.INSTANCE && !fileType.isReadOnly();
    }

    // -------------------------------------------------------------------------
    // Setup
    // -------------------------------------------------------------------------

    public FileTypeAssocTable<FileType> getExtensionMap() {
        return myPatternsTable;
    }

    public void setPatternsTable(Set<? extends FileType> fileTypes, FileTypeAssocTable<FileType> assocTable) {
        Map<FileNameMatcher, FileType> removedMappings = getExtensionMap().getRemovedMappings(assocTable, fileTypes);
        fireBeforeFileTypesChanged();
        for (FileType existing : getRegisteredFileTypes()) {
            if (!fileTypes.contains(existing)) {
                mySchemeManager.removeScheme(existing);
            }
        }
        for (FileType fileType : fileTypes) {
            mySchemeManager.addNewScheme(fileType, true);
            if (fileType instanceof AbstractFileType abstractFileType) {
                abstractFileType.initSupport();
            }
        }
        myPatternsTable = assocTable.copy();
        fireFileTypesChanged();

        myRemovedMappingTracker.removeMatching((matcher, fileTypeName) -> {
            FileType fileType = getFileTypeByName(fileTypeName);
            return fileType != null && assocTable.isAssociatedWith(fileType, matcher);
        });
        for (Map.Entry<FileNameMatcher, FileType> entry : removedMappings.entrySet()) {
            myRemovedMappingTracker.add(entry.getKey(), entry.getValue().getId(), true);
        }
    }

    public void associate(FileType fileType, FileNameMatcher matcher, boolean fireChange) {
        if (!myPatternsTable.isAssociatedWith(fileType, matcher)) {
            if (fireChange) {
                fireBeforeFileTypesChanged();
            }
            myPatternsTable.addAssociation(matcher, fileType);
            if (fireChange) {
                fireFileTypesChanged();
            }
        }
    }

    public void removeAssociation(FileType fileType, FileNameMatcher matcher, boolean fireChange) {
        if (myPatternsTable.isAssociatedWith(fileType, matcher)) {
            if (fireChange) {
                fireBeforeFileTypesChanged();
            }
            myPatternsTable.removeAssociation(matcher, fileType);
            if (fireChange) {
                fireFileTypesChanged();
            }
        }
    }

    @Override
    @RequiredUIAccess
    public @Nullable FileType getKnownFileTypeOrAssociate(VirtualFile file) {
        FileType type = file.getFileType();
        if (type == UnknownFileType.INSTANCE) {
            type = FileTypeChooser.associateFileType(file.getName());
        }

        return type;
    }

    @Override
    @RequiredUIAccess
    public FileType getKnownFileTypeOrAssociate(VirtualFile file, Project project) {
        return FileTypeChooser.getKnownFileTypeOrAssociate(file, project);
    }

    @Override
    @RequiredUIAccess
    public @Nullable FileType getKnownFileTypeOrAssociate(String fileName) {
        return FileTypeChooser.getKnownFileTypeOrAssociate(fileName);
    }

    private void registerReDetectedMappings(StandardFileType pair) {
        FileType fileType = pair.fileType;
        if (fileType == PlainTextFileType.INSTANCE) {
            return;
        }
        for (FileNameMatcher matcher : pair.matchers) {
            registerReDetectedMapping(fileType.getId(), matcher);
            if (matcher instanceof ExtensionFileNameMatcherImpl extMatcher) {
                // also check exact file name matcher
                registerReDetectedMapping(fileType.getId(), new ExactFileNameMatcherImpl("." + extMatcher.getExtension()));
            }
        }
    }

    private void registerReDetectedMapping(String fileTypeName, FileNameMatcher matcher) {
        String typeName = myUnresolvedMappings.get(matcher);
        if (typeName != null && !typeName.equals(fileTypeName)) {
            if (!myRemovedMappingTracker.hasRemovedMapping(matcher)) {
                myRemovedMappingTracker.add(matcher, fileTypeName, false);
            }
            myUnresolvedMappings.remove(matcher);
        }
    }

    RemovedMappingTracker getRemovedMappingTracker() {
        return myRemovedMappingTracker;
    }

    @TestOnly
    void clearForTests() {
        for (StandardFileType fileType : myStandardFileTypes.values()) {
            myPatternsTable.removeAllAssociations(fileType.fileType);
        }
        for (FileType type : myDefaultTypes) {
            myPatternsTable.removeAllAssociations(type);
        }
        myStandardFileTypes.clear();
        myDefaultTypes.clear();
        myUnresolvedMappings.clear();
        myRemovedMappingTracker.clear();
        mySchemeManager.clearAllSchemes();
    }

    @Override
    public void dispose() {
        myDetectionService.logStatistics();
    }

    @Override
    public @Nullable FileType getFileTypeByMimeType(@Nullable String mimeType) {
        for (Language language : Language.getRegisteredLanguages()) {
            String[] types = language.getMimeTypes();
            for (String type : types) {
                if (type.equalsIgnoreCase(mimeType)) {
                    FileType fileType = language.getAssociatedFileType();
                    if (fileType != null) {
                        return fileType;
                    }
                }
            }
        }
        return null;
    }

}
