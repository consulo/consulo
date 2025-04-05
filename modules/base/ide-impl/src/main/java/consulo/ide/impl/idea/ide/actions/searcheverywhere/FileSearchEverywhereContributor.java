// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.actions.searcheverywhere;

import consulo.fileEditor.impl.internal.OpenFileDescriptorImpl;
import consulo.ide.impl.idea.ide.actions.GotoFileAction;
import consulo.ide.impl.idea.ide.util.gotoByName.FilteringGotoByModel;
import consulo.ide.impl.idea.ide.util.gotoByName.GotoFileConfiguration;
import consulo.ide.impl.idea.ide.util.gotoByName.GotoFileModel;
import consulo.ide.localize.IdeLocalize;
import consulo.language.file.FileTypeManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiFileSystemItem;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.ex.action.AnAction;
import consulo.util.dataholder.Key;
import consulo.util.io.FileUtil;
import consulo.util.lang.Couple;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Konstantin Bulenkov
 * @author Mikhail Sokolov
 */
public class FileSearchEverywhereContributor extends AbstractGotoSEContributor {
    private final GotoFileModel myModelForRenderer;
    private final PersistentSearchEverywhereContributorFilter<FileType> myFilter;

    public FileSearchEverywhereContributor(@Nullable Project project, @Nullable PsiElement context) {
        super(project, context);
        myModelForRenderer = project == null ? null : new GotoFileModel(project);
        myFilter = project == null ? null : createFileTypeFilter(project);
    }

    @Nonnull
    @Override
    public String getGroupName() {
        return "Files";
    }

    public LocalizeValue includeNonProjectItemsText() {
        return IdeLocalize.checkboxIncludeNonProjectFiles();
    }

    @Override
    public int getSortWeight() {
        return 200;
    }

    @Override
    public int getElementPriority(@Nonnull Object element, @Nonnull String searchPattern) {
        return super.getElementPriority(element, searchPattern) + 2;
    }

    @Nonnull
    @Override
    protected FilteringGotoByModel<FileType> createModel(@Nonnull Project project) {
        GotoFileModel model = new GotoFileModel(project);
        if (myFilter != null) {
            model.setFilterItems(myFilter.getSelectedElements());
        }
        return model;
    }

    @Nonnull
    @Override
    public List<AnAction> getActions(@Nonnull Runnable onChanged) {
        return doGetActions(includeNonProjectItemsText(), myFilter, onChanged);
    }

    @Nonnull
    @Override
    @SuppressWarnings("unchecked")
    public ListCellRenderer<Object> getElementsRenderer() {
        return (ListCellRenderer)new SERenderer() {
            @Nonnull
            @Override
            protected ItemMatchers getItemMatchers(@Nonnull JList list, @Nonnull Object value) {
                ItemMatchers defaultMatchers = super.getItemMatchers(list, value);
                if (!(value instanceof PsiFileSystemItem) || myModelForRenderer == null) {
                    return defaultMatchers;
                }

                return GotoFileModel.convertToFileItemMatchers(defaultMatchers, (PsiFileSystemItem)value, myModelForRenderer);
            }
        };
    }

    @Override
    public boolean processSelectedItem(@Nonnull Object selected, int modifiers, @Nonnull String searchText) {
        if (selected instanceof PsiFile file) {
            VirtualFile virtualFile = file.getVirtualFile();
            if (virtualFile != null && myProject != null) {
                Couple<Integer> pos = getLineAndColumn(searchText);
                OpenFileDescriptorImpl descriptor = new OpenFileDescriptorImpl(myProject, virtualFile, pos.first, pos.second);
                descriptor.setUseCurrentWindow(openInCurrentWindow(modifiers));
                if (descriptor.canNavigate()) {
                    descriptor.navigate(true);
                    return true;
                }
            }
        }

        return super.processSelectedItem(selected, modifiers, searchText);
    }

    @Override
    public Object getDataForItem(@Nonnull Object element, @Nonnull Key dataId) {
        if (PsiFile.KEY == dataId && element instanceof PsiFile) {
            return element;
        }

        if (SearchEverywhereDataKeys.ITEM_STRING_DESCRIPTION == dataId && element instanceof PsiFile file) {
            String path = file.getVirtualFile().getPath();
            path = FileUtil.toSystemIndependentName(path);
            if (myProject != null) {
                String basePath = myProject.getBasePath();
                if (basePath != null) {
                    path = FileUtil.getRelativePath(basePath, path, '/');
                }
            }
            return path;
        }

        return super.getDataForItem(element, dataId);
    }

    @Nonnull
    public static PersistentSearchEverywhereContributorFilter<FileType> createFileTypeFilter(@Nonnull Project project) {
        List<FileType> items = Stream.of(FileTypeManager.getInstance().getRegisteredFileTypes())
            .sorted(GotoFileAction.FileTypeComparator.INSTANCE)
            .collect(Collectors.toList());
        return new PersistentSearchEverywhereContributorFilter<>(
            items,
            GotoFileConfiguration.getInstance(project),
            FileType::getId,
            FileType::getIcon
        );
    }
}
