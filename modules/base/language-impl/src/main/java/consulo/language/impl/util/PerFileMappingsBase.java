// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.impl.util;

import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.component.persist.PersistentStateComponent;
import consulo.disposer.Disposable;
import consulo.language.file.inject.VirtualFileWindow;
import consulo.language.file.light.LightVirtualFile;
import consulo.language.psi.PsiDocumentManager;
import consulo.module.content.FilePropertyPusher;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.PushedFilePropertiesUpdater;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.undoRedo.BasicUndoableAction;
import consulo.undoRedo.CommandProcessor;
import consulo.undoRedo.ProjectUndoManager;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.JBIterable;
import consulo.util.dataholder.Key;
import consulo.util.lang.Comparing;
import consulo.util.lang.ref.SoftReference;
import consulo.virtualFileSystem.NonPhysicalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import consulo.virtualFileSystem.event.BulkFileListener;
import consulo.virtualFileSystem.event.VFileDeleteEvent;
import consulo.virtualFileSystem.event.VFileEvent;
import consulo.virtualFileSystem.util.PerFileMappingsEx;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jdom.Element;
import org.jetbrains.annotations.TestOnly;

import java.lang.ref.WeakReference;
import java.util.*;

/**
 * @author gregsh
 */
public abstract class PerFileMappingsBase<T> implements PersistentStateComponent<Element>, PerFileMappingsEx<T>, Disposable {
    private final Project myProject;
    private List<PerFileMappingState> myDeferredMappings;
    private final Map<VirtualFile, T> myMappings = new HashMap<>();

    public PerFileMappingsBase() {
        this(null);
    }

    public PerFileMappingsBase(@Nullable Project project) {
        myProject = project;
        installDeleteUndo();
    }

    @Override
    public void dispose() {
    }

    @Nullable
    protected FilePropertyPusher<T> getFilePropertyPusher() {
        return null;
    }

    @Nullable
    protected Project getProject() {
        return myProject;
    }

    @Nonnull
    @Override
    public Map<VirtualFile, T> getMappings() {
        synchronized (myMappings) {
            ensureStateLoaded();
            cleanup();
            return Collections.unmodifiableMap(myMappings);
        }
    }

    private void cleanup() {
        myMappings.keySet().removeIf(file -> file != null /* PROJECT, top-level */ && !file.isValid());
    }

    @Override
    @Nullable
    public T getMapping(@Nullable VirtualFile file) {
        T t = getConfiguredMapping(file);
        return t == null ? getDefaultMapping(file) : t;
    }

    @Nullable
    public T getConfiguredMapping(@Nullable VirtualFile file) {
        FilePropertyPusher<T> pusher = getFilePropertyPusher();
        return getMappingInner(file, pusher == null ? null : pusher.getFileDataKey(), false);
    }

    @Nullable
    public T getDirectlyConfiguredMapping(@Nullable VirtualFile file) {
        return getMappingInner(file, null, true);
    }

    @Nullable
    private T getMappingInner(@Nullable VirtualFile file, @Nullable Key<T> pusherKey, boolean forHierarchy) {
        if (file instanceof VirtualFileWindow) {
            VirtualFileWindow window = (VirtualFileWindow) file;
            file = window.getDelegate();
        }
        VirtualFile originalFile = file instanceof LightVirtualFile lightVirtualFile ? lightVirtualFile.getOriginalFile() : null;
        if (Comparing.equal(originalFile, file)) {
            originalFile = null;
        }

        if (file != null) {
            T pushedValue = pusherKey == null ? null : file.getUserData(pusherKey);
            if (pushedValue != null) {
                return pushedValue;
            }
        }
        if (originalFile != null) {
            T pushedValue = pusherKey == null ? null : originalFile.getUserData(pusherKey);
            if (pushedValue != null) {
                return pushedValue;
            }
        }
        synchronized (myMappings) {
            ensureStateLoaded();
            T t = getMappingForHierarchy(file, myMappings);
            if (t != null) {
                return t;
            }
            t = getMappingForHierarchy(originalFile, myMappings);
            if (t != null) {
                return t;
            }
            if (forHierarchy && file != null) {
                return null;
            }
            return getNotInHierarchy(originalFile != null ? originalFile : file, myMappings);
        }
    }

    @Nullable
    protected T getNotInHierarchy(@Nullable VirtualFile file, @Nonnull Map<VirtualFile, T> mappings) {
        if (getProject() == null || file == null || file.getFileSystem() instanceof NonPhysicalFileSystem || !getProject().isDefault() && ProjectFileIndex.getInstance(
            getProject()).isInContent(file)) {
            return mappings.get(null);
        }
        return null;
    }

    private static <T> T getMappingForHierarchy(@Nullable VirtualFile file, @Nonnull Map<VirtualFile, T> mappings) {
        for (VirtualFile cur = file; cur != null; cur = cur.getParent()) {
            T t = mappings.get(cur);
            if (t != null) {
                return t;
            }
        }
        return null;
    }

    @Nullable
    @Override
    public T getDefaultMapping(@Nullable VirtualFile file) {
        return null;
    }

    @Nullable
    @Override
    public T getImmediateMapping(@Nullable VirtualFile file) {
        synchronized (myMappings) {
            ensureStateLoaded();
            return myMappings.get(file);
        }
    }

    @Override
    public void setMappings(@Nonnull Map<VirtualFile, T> mappings) {
        Collection<VirtualFile> oldFiles;
        synchronized (myMappings) {
            myDeferredMappings = null;
            oldFiles = new ArrayList<>(myMappings.keySet());
            myMappings.clear();
            myMappings.putAll(mappings);
            cleanup();
        }
        Project project = getProject();
        handleMappingChange(mappings.keySet(), oldFiles, project != null && !project.isDefault());
    }

    @Override
    public void setMapping(@Nullable VirtualFile file, @Nullable T value) {
        synchronized (myMappings) {
            ensureStateLoaded();
            if (value == null) {
                myMappings.remove(file);
            }
            else {
                myMappings.put(file, value);
            }
        }
        List<VirtualFile> files = ContainerUtil.createMaybeSingletonList(file);
        handleMappingChange(files, files, false);
    }

    private void handleMappingChange(
        Collection<? extends VirtualFile> files,
        Collection<? extends VirtualFile> oldFiles,
        boolean includeOpenFiles
    ) {
        Project project = getProject();
        FilePropertyPusher<T> pusher = getFilePropertyPusher();
        if (project != null && pusher != null) {
            for (VirtualFile oldFile : oldFiles) {
                if (oldFile == null) {
                    continue; // project
                }
                oldFile.putUserData(pusher.getFileDataKey(), null);
            }
            if (!project.isDefault()) {
                PushedFilePropertiesUpdater.getInstance(project).pushAll(pusher);
            }
        }
        if (shouldReparseFiles()) {
            Project[] projects = project == null ? ProjectManager.getInstance().getOpenProjects() : new Project[]{project};
            for (Project p : projects) {
                PsiDocumentManager.getInstance(p).reparseFiles(files, includeOpenFiles);
            }
        }
    }

    @Nonnull
    public abstract List<T> getAvailableValues();

    @Nullable
    protected abstract String serialize(T t);

    @Override
    public Element getState() {
        synchronized (myMappings) {
            if (myDeferredMappings != null) {
                return PerFileMappingState.write(myDeferredMappings, getValueAttribute());
            }

            cleanup();
            Element element = new Element("x");
            List<VirtualFile> files = new ArrayList<>(myMappings.keySet());
            files.sort((o1, o2) -> {
                if (o1 == null || o2 == null) {
                    return o1 == null ? o2 == null ? 0 : 1 : -1;
                }
                return o1.getPath().compareTo(o2.getPath());
            });
            for (VirtualFile file : files) {
                T value = myMappings.get(file);
                String valueStr = value == null ? null : serialize(value);
                if (valueStr == null) {
                    continue;
                }
                Element child = new Element("file");
                element.addContent(child);
                child.setAttribute("url", file == null ? "PROJECT" : file.getUrl());
                child.setAttribute(getValueAttribute(), valueStr);
            }
            return element;
        }
    }

    @Nullable
    protected T handleUnknownMapping(VirtualFile file, String value) {
        return null;
    }

    @SuppressWarnings("DeprecatedIsStillUsed")
    @Nonnull
    @Deprecated
    // better to not override
    protected String getValueAttribute() {
        return "value";
    }

    @Override
    public void loadState(@Nonnull Element element) {
        // read not under lock
        List<PerFileMappingState> list = PerFileMappingState.read(element, getValueAttribute());
        synchronized (myMappings) {
            if (list.isEmpty()) {
                myMappings.clear();
                myDeferredMappings = null;
            }
            else {
                myDeferredMappings = list;
            }
        }
    }

    private void ensureStateLoaded() {
        synchronized (myMappings) {
            List<PerFileMappingState> state = myDeferredMappings;
            if (state == null) {
                return;
            }

            myDeferredMappings = null;
            Map<String, T> valuesMap = new HashMap<>();
            for (T value : getAvailableValues()) {
                String key = serialize(value);
                if (key != null) {
                    valuesMap.put(key, value);
                }
            }
            myMappings.clear();
            for (PerFileMappingState entry : state) {
                String url = entry.getUrl();
                String valueStr = entry.getValue();
                VirtualFile file = "PROJECT".equals(url) ? null : VirtualFileManager.getInstance().findFileByUrl(url);
                T value = valuesMap.get(valueStr);
                if (value == null) {
                    value = handleUnknownMapping(file, valueStr);
                    if (value == null) {
                        continue;
                    }
                }
                if (file != null || url.equals("PROJECT")) {
                    myMappings.put(file, value);
                }
            }
        }
    }

    @TestOnly
    public void cleanupForNextTest() {
        synchronized (myMappings) {
            myDeferredMappings = null;
            myMappings.clear();
        }
    }

    protected boolean shouldReparseFiles() {
        return true;
    }

    public boolean hasMappings() {
        synchronized (myMappings) {
            ensureStateLoaded();
            return !myMappings.isEmpty();
        }
    }

    private void installDeleteUndo() {
        Application app = ApplicationManager.getApplication();
        if (app == null) {
            return;
        }
        app.getMessageBus().connect(this).subscribe(
            BulkFileListener.class,
            new BulkFileListener() {
                WeakReference<MyUndoableAction> lastAction;

                @Override
                public void before(@Nonnull List<? extends VFileEvent> events) {
                    if (CommandProcessor.getInstance().isUndoTransparentActionInProgress()) {
                        return;
                    }
                    Project project = CommandProcessor.getInstance().getCurrentCommandProject();
                    if (project == null || !project.isOpen()) {
                        return;
                    }

                    MyUndoableAction action = createUndoableAction(events);
                    if (action == null) {
                        return;
                    }
                    action.doRemove(action.removed);
                    lastAction = new WeakReference<>(action);
                    ProjectUndoManager.getInstance(project).undoableActionPerformed(action);
                }

                @Override
                public void after(@Nonnull List<? extends VFileEvent> events) {
                    MyUndoableAction action = SoftReference.dereference(lastAction);
                    lastAction = null;
                    if (action != null) {
                        Application.get().invokeLater(() -> action.doAdd(action.added));
                    }
                }

                @Nullable
                MyUndoableAction createUndoableAction(@Nonnull List<? extends VFileEvent> events) {
                    // NOTE: VFS handles renames, so the code for RENAME events is deleted (see history)
                    List<? extends VFileEvent> eventsFiltered = JBIterable.from(events).filter(VFileDeleteEvent.class).toList();
                    if (eventsFiltered.isEmpty()) {
                        return null;
                    }

                    Map<String, T> removed = new HashMap<>();
                    NavigableSet<VirtualFile> navSet = null;

                    synchronized (myMappings) {
                        ensureStateLoaded();
                        for (VFileEvent event : eventsFiltered) {
                            VirtualFile file = event.getFile();
                            if (file == null) {
                                continue;
                            }
                            String fileUrl = file.getUrl();
                            if (!file.isDirectory()) {
                                T m = myMappings.get(file);
                                if (m != null) {
                                    removed.put(fileUrl, m);
                                }
                            }
                            else {
                                if (navSet == null) {
                                    navSet = new TreeSet<>((f1, f2) -> Comparing.compare(
                                        f1 == null ? null : f1.getUrl(),
                                        f2 == null ? null : f2.getUrl()
                                    ));
                                    navSet.addAll(myMappings.keySet());
                                }
                                for (VirtualFile child : navSet.tailSet(file)) {
                                    if (!VirtualFileUtil.isAncestor(file, child, false)) {
                                        break;
                                    }
                                    String childUrl = child.getUrl();
                                    T m = myMappings.get(child);
                                    removed.put(childUrl, m);
                                }
                            }
                        }
                    }
                    return removed.isEmpty() ? null : new MyUndoableAction(new HashMap<>(), removed);
                }
            }
        );
    }

    private class MyUndoableAction extends BasicUndoableAction {
        final Map<String, T> added;
        final Map<String, T> removed;

        MyUndoableAction(Map<String, T> added, Map<String, T> removed) {
            this.added = added;
            this.removed = removed;
        }

        @Override
        public void undo() {
            doRemove(added);
            doAdd(removed);
        }

        @Override
        public void redo() {
            doRemove(removed);
            doAdd(added);
        }

        void doAdd(Map<String, T> toAdd) {
            if (toAdd == null) {
                return;
            }
            for (String url : toAdd.keySet()) {
                VirtualFile file = VirtualFileManager.getInstance().findFileByUrl(url);
                if (file == null) {
                    continue;
                }
                setMapping(file, toAdd.get(url));
            }
        }

        void doRemove(Map<String, T> toRemove) {
            if (toRemove != null) {
                synchronized (myMappings) {
                    for (String url : toRemove.keySet()) {
                        VirtualFile file = VirtualFileManager.getInstance().findFileByUrl(url);
                        if (file == null) {
                            continue;
                        }
                        myMappings.remove(file);
                    }
                }
            }
        }
    }
}
