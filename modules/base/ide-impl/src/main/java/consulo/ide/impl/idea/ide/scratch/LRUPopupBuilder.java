// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.scratch;

import consulo.ide.impl.idea.ide.util.PropertiesComponent;
import consulo.language.Language;
import consulo.language.editor.scratch.ScratchUtil;
import consulo.language.file.LanguageFileType;
import consulo.language.localize.LanguageLocalize;
import consulo.language.util.LanguageUtil;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.ex.awt.EmptyIcon;
import consulo.ui.ex.popup.*;
import consulo.ui.image.Image;
import consulo.undoRedo.BasicUndoableAction;
import consulo.undoRedo.CommandProcessor;
import consulo.undoRedo.ProjectUndoManager;
import consulo.undoRedo.UnexpectedUndoException;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.JBIterable;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.ReadonlyStatusHandler;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.PerFileMappings;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.List;
import java.util.*;
import java.util.function.Consumer;

/**
 * @author gregsh
 */
public abstract class LRUPopupBuilder<T> {
    private static final Logger LOG = Logger.getInstance(LRUPopupBuilder.class);
    private static final int MAX_VISIBLE_SIZE = 20;
    private static final int LRU_ITEMS = 4;

    private final String myTitle;
    private final PropertiesComponent myPropertiesComponent;
    private final Map<T, Pair<String, Image>> myPresentations = new IdentityHashMap<>();

    private T mySelection;
    private Consumer<? super T> myOnChosen;
    private Comparator<? super T> myComparator;
    private Iterable<? extends T> myItemsIterable;
    private JBIterable<T> myExtraItems = JBIterable.empty();

    
    public static ListPopup forFileLanguages(
        Project project,
        String title,
        Iterable<? extends VirtualFile> files,
        PerFileMappings<Language> mappings
    ) {
        VirtualFile[] filesCopy = VirtualFileUtil.toVirtualFileArray(JBIterable.from(files).toList());
        Arrays.sort(filesCopy, (o1, o2) -> StringUtil.compare(o1.getName(), o2.getName(), !o1.getFileSystem().isCaseSensitive()));
        return forFileLanguages(
            project,
            title,
            null,
            t -> {
                try {
                    CommandProcessor.getInstance().newCommand()
                        .project(project)
                        .name(LanguageLocalize.commandNameChangeLanguage())
                        .inWriteAction()
                        .canThrow(UnexpectedUndoException.class)
                        .run(() -> changeLanguageWithUndo(project, t, filesCopy, mappings));
                }
                catch (UnexpectedUndoException e) {
                    LOG.error(e);
                }
            }
        );
    }

    /**
     * @deprecated use {@link #forFileLanguages(Project, String, Language, Consumer)}
     */
    @Deprecated
    
    public static ListPopup forFileLanguages(
        Project project,
        @Nullable Language selection,
        Consumer<? super Language> onChosen
    ) {
        return forFileLanguages(project, "Languages", selection, onChosen);
    }

    
    public static ListPopup forFileLanguages(
        Project project,
        String title,
        @Nullable Language selection,
        Consumer<? super Language> onChosen
    ) {
        return languagePopupBuilder(project, title).
            forValues(LanguageUtil.getFileLanguages()).
            withSelection(selection).
            onChosen(onChosen).
            buildPopup();
    }

    
    public static LRUPopupBuilder<Language> languagePopupBuilder(Project project, String title) {
        return new LRUPopupBuilder<Language>(project, title) {
            @Override
            public String getDisplayName(Language language) {
                return language.getDisplayName().get();
            }

            @Override
            public Image getIcon(Language language) {
                LanguageFileType associatedLanguage = language.getAssociatedFileType();
                return associatedLanguage != null ? associatedLanguage.getIcon() : null;
            }

            @Override
            public String getStorageId(Language language) {
                return language.getID();
            }
        }.withComparator(LanguageUtil.LANGUAGE_COMPARATOR);
    }

    protected LRUPopupBuilder(Project project, String title) {
        myTitle = title;
        myPropertiesComponent = PropertiesComponent.getInstance(project);
    }

    public abstract String getDisplayName(T t);

    public abstract String getStorageId(T t);

    public abstract Image getIcon(T t);

    
    public LRUPopupBuilder<T> forValues(@Nullable Iterable<? extends T> items) {
        myItemsIterable = items;
        return this;
    }

    
    public LRUPopupBuilder<T> withSelection(@Nullable T t) {
        mySelection = t;
        return this;
    }

    
    public LRUPopupBuilder<T> withExtra(T extra, String displayName, @Nullable Image icon) {
        myExtraItems = myExtraItems.append(extra);
        myPresentations.put(extra, Pair.create(displayName, icon));
        return this;
    }

    
    public LRUPopupBuilder<T> onChosen(@Nullable Consumer<? super T> consumer) {
        myOnChosen = consumer;
        return this;
    }

    public LRUPopupBuilder<T> withComparator(@Nullable Comparator<? super T> comparator) {
        myComparator = comparator;
        return this;
    }

    
    public ListPopup buildPopup() {
        List<String> ids = ContainerUtil.newArrayList(restoreLRUItems());
        if (mySelection != null) {
            ids.add(getStorageId(mySelection));
        }
        List<T> lru = new ArrayList<>(LRU_ITEMS);
        List<T> items = new ArrayList<>(MAX_VISIBLE_SIZE);
        List<T> extra = myExtraItems.toList();
        if (myItemsIterable != null) {
            for (T t : myItemsIterable) {
                (ids.contains(getStorageId(t)) ? lru : items).add(t);
            }
        }
        if (myComparator != null) {
            items.sort(myComparator);
        }
        if (!lru.isEmpty()) {
            lru.sort(Comparator.comparingInt(o -> ids.indexOf(getStorageId(o))));
        }
        T separator1 = !lru.isEmpty() && !items.isEmpty() ? items.get(0) : null;
        T separator2 = !lru.isEmpty() || !items.isEmpty() ? ContainerUtil.getFirstItem(extra) : null;

        List<T> combinedItems = ContainerUtil.concat(lru, items, extra);
        BaseListPopupStep<T> step = new BaseListPopupStep<T>(myTitle, combinedItems) {
            
            @Override
            public String getTextFor(T t) {
                return t == null ? "" : getPresentation(t).first;
            }

            @Override
            public Image getIconFor(T t) {
                return t == null ? null : getPresentation(t).second;
            }

            @Override
            public boolean isSpeedSearchEnabled() {
                return true;
            }

            @Override
            public PopupStep onChosen(T t, boolean finalChoice) {
                if (!extra.contains(t)) {
                    storeLRUItems(t);
                }
                if (myOnChosen != null) {
                    doFinalStep(() -> myOnChosen.accept(t));
                }
                return null;
            }

            @Nullable
            @Override
            public ListSeparator getSeparatorAbove(T value) {
                return value == separator1 || value == separator2 ? new ListSeparator() : null;
            }
        };
        int selection = Math.max(0, mySelection != null ? combinedItems.indexOf(mySelection) : 0);
        step.setDefaultOptionIndex(selection);

        return tweakSizeToPreferred(JBPopupFactory.getInstance().createListPopup(step));
    }

    
    private Pair<String, Image> getPresentation(T t) {
        Pair<String, Image> p = myPresentations.get(t);
        if (p == null) {
            myPresentations.put(t, p = Pair.create(getDisplayName(t), getIcon(t)));
        }
        return p;
    }

    
    private static ListPopup tweakSizeToPreferred(ListPopup popup) {
        int nameLen = 0;
        ListPopupStep step = popup.getListStep();
        List values = step.getValues();
        for (Object v : values) {
            //noinspection unchecked
            nameLen = Math.max(nameLen, step.getTextFor(v).length());
        }
        if (values.size() > MAX_VISIBLE_SIZE) {
            Dimension size =
                new JLabel(StringUtil.repeatSymbol('a', nameLen), EmptyIcon.ICON_16, SwingConstants.LEFT).getPreferredSize();
            size.width += 20;
            size.height *= MAX_VISIBLE_SIZE;
            popup.setSize(size);
        }
        return popup;
    }

    
    private String[] restoreLRUItems() {
        return ObjectUtil.notNull(myPropertiesComponent.getValues(getLRUKey()), ArrayUtil.EMPTY_STRING_ARRAY);
    }

    private void storeLRUItems(T t) {
        String[] values = myPropertiesComponent.getValues(getLRUKey());
        List<String> lastUsed = new ArrayList<>(LRU_ITEMS);
        lastUsed.add(getStorageId(t));
        if (values != null) {
            for (String value : values) {
                if (!lastUsed.contains(value)) {
                    lastUsed.add(value);
                }
                if (lastUsed.size() == LRU_ITEMS) {
                    break;
                }
            }
        }
        myPropertiesComponent.setValues(getLRUKey(), ArrayUtil.toStringArray(lastUsed));
    }

    
    private String getLRUKey() {
        return getClass().getName() + "/" + myTitle;
    }

    private static void changeLanguageWithUndo(
        Project project,
        Language t,
        VirtualFile[] sortedFiles,
        PerFileMappings<Language> mappings
    ) throws UnexpectedUndoException {
        ReadonlyStatusHandler.OperationStatus status =
            ReadonlyStatusHandler.getInstance(project).ensureFilesWritable(Arrays.asList(sortedFiles));
        if (status.hasReadonlyFiles()) {
            return;
        }

        Set<VirtualFile> matchedExtensions = new LinkedHashSet<>();
        final Map<VirtualFile, Language> oldMapping = new HashMap<>();
        for (VirtualFile file : sortedFiles) {
            oldMapping.put(file, mappings.getMapping(file));
            if (ScratchUtil.hasMatchingExtension(project, file)) {
                matchedExtensions.add(file);
            }
        }

        BasicUndoableAction action = new BasicUndoableAction(sortedFiles) {
            @Override
            public void undo() {
                for (VirtualFile file : sortedFiles) {
                    mappings.setMapping(file, oldMapping.get(file));
                }
            }

            @Override
            public void redo() {
                for (VirtualFile file : sortedFiles) {
                    mappings.setMapping(file, t);
                }
            }
        };
        action.redo();
        ProjectUndoManager.getInstance(project).undoableActionPerformed(action);

        for (VirtualFile file : matchedExtensions) {
            try {
                ScratchUtil.updateFileExtension(project, file);
            }
            catch (IOException ignored) {
            }
        }
    }
}
