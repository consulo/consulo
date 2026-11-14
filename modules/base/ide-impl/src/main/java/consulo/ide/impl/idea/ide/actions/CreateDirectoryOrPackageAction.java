/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ide.actions;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ActionImpl;
import consulo.application.WriteAction;
import consulo.application.util.matcher.MatcherTextRange;
import consulo.application.util.matcher.MinusculeMatcher;
import consulo.application.util.matcher.NameUtil;
import consulo.component.extension.ExtensionPoint;
import consulo.content.ContentFolderTypeProvider;
import consulo.ide.action.CreateDirectoryCompletionContributor;
import consulo.ide.action.ui.NewItemPopupUtil;
import consulo.ide.action.ui.NewItemWithTemplatesPopupPanel;
import consulo.ide.impl.actions.CreateDirectoryOrPackageType;
import consulo.ide.impl.idea.ide.projectView.actions.MarkRootAction;
import consulo.language.editor.util.IdeView;
import consulo.ide.localize.IdeLocalize;
import consulo.ide.util.DirectoryChooserUtil;
import consulo.language.localize.LanguageLocalize;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFileSystemItem;
import consulo.language.psi.PsiPackageSupportProvider;
import consulo.localize.LocalizeValue;
import consulo.module.Module;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.ProjectRootManager;
import consulo.module.content.layer.ContentEntry;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.module.extension.ModuleExtension;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.HasValidator;
import consulo.ui.TextAttribute;
import consulo.ui.TextBox;
import consulo.ui.TextItemPresentation;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.InputValidatorEx;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.LegacyDumbAwareAction;
import consulo.ui.ex.action.Presentation;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.image.Image;
import consulo.ui.style.ComponentColors;
import consulo.util.collection.FList;
import consulo.util.io.FileUtil;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import consulo.util.lang.Trinity;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

@ActionImpl(id = "NewDir")
public class CreateDirectoryOrPackageAction extends LegacyDumbAwareAction {
    public CreateDirectoryOrPackageAction() {
        super(IdeLocalize.actionCreateNewDirectoryOrPackage(), IdeLocalize.actionCreateNewDirectoryOrPackage());
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        IdeView view = e.getRequiredData(IdeView.KEY);
        Project project = e.getRequiredData(Project.KEY);

        PsiDirectory directory = DirectoryChooserUtil.getOrChooseDirectory(view);

        if (directory == null) {
            return;
        }

        Trinity<ContentFolderTypeProvider, PsiDirectory, CreateDirectoryOrPackageType> info = getInfo(directory);

        boolean isDirectory = info.getThird() == CreateDirectoryOrPackageType.Directory;

        CreateDirectoryOrPackageHandler validator =
            new CreateDirectoryOrPackageHandler(project, directory, info.getThird(), info.getThird().getSeparator());

        LocalizeValue title = isDirectory ? IdeLocalize.titleNewDirectory() : IdeLocalize.titleNewPackage();

        String defaultValue = info.getThird().getDefaultValue(directory);

        createLightWeightPopup(
            directory,
            isDirectory,
            validator,
            title,
            defaultValue,
            element -> {
                if (element != null) {
                    view.selectElement(element);
                }
            }
        ).showCenteredInCurrentWindow(project);
    }

    @RequiredUIAccess
    private JBPopup createLightWeightPopup(
        PsiDirectory directory,
        boolean isDirectory,
        CreateDirectoryOrPackageHandler validator,
        LocalizeValue title,
        String defaultValue,
        Consumer<PsiElement> consumer
    ) {
        // package names are created from the content folder root, the suggestions are relative to the selected directory
        List<CompletionItem> variants = isDirectory ? collectSuggestedDirectories(directory) : List.of();
        DirectoriesWithCompletionPopupPanel contentPanel = new DirectoriesWithCompletionPopupPanel(variants);

        TextBox nameField = contentPanel.getTextField();
        if (!StringUtil.isEmptyOrSpaces(defaultValue)) {
            nameField.setValue(defaultValue);
        }
        JBPopup popup = NewItemPopupUtil.createNewItemPopup(title, contentPanel.getComponent(), nameField);
        contentPanel.addValidator(value -> {
            if (!validator.checkInput(value)) {
                String message = InputValidatorEx.getErrorText(validator, value, LanguageLocalize.incorrectName().get());
                return new HasValidator.ValidationInfo(message);
            }

            return null;
        });

        contentPanel.setApplyAction(event -> {
            // if there are selected suggestions, we need to create the selected folders (not the path in the text field)
            List<Pair<String, @Nullable ContentFolderTypeProvider>> toCreate = new ArrayList<>();
            for (CompletionItem item : contentPanel.getSelectedItems()) {
                toCreate.add(Pair.create(item.myRelativePath, item.myRootType));
            }

            // when there are no selected suggestions, simply create the directory with the path from the text field
            if (toCreate.isEmpty()) {
                toCreate = Collections.singletonList(Pair.create(nameField.getValue(), null));
            }

            List<PsiElement> created = createDirectories(toCreate, validator);
            if (created != null) {
                popup.closeOk(event);
                // we don't have API for multi-selection in the views,
                // so let's at least make sure the created elements are visible, and the first one is selected
                for (PsiElement element : created.reversed()) {
                    consumer.accept(element);
                }
            }
        });

        UIAccess uiAccess = UIAccess.current();
        contentPanel.addTemplatesVisibilityListener(visible -> {
            // The re-layout should be delayed since we are in the middle of model changes processing and not all components updated their states
            uiAccess.give(() -> popup.pack(false, true));
        });

        return popup;
    }

    @RequiredReadAction
    protected List<CompletionItem> collectSuggestedDirectories(PsiDirectory directory) {
        List<CompletionItem> variants = new ArrayList<>();

        VirtualFile vDir = directory.getVirtualFile();
        directory.getProject().getApplication().getExtensionPoint(CreateDirectoryCompletionContributor.class).forEach(contributor -> {
            for (CreateDirectoryCompletionContributor.Variant variant : contributor.getVariants(directory)) {
                String relativePath = FileUtil.toSystemIndependentName(variant.getPath());
                if (FileUtil.isAbsolutePlatformIndependent(relativePath)) {
                    // only suggest sub-folders
                    if (!FileUtil.isAncestor(vDir.getPath(), relativePath, true)) {
                        continue;
                    }

                    // convert absolute paths to the relative paths
                    relativePath = FileUtil.getRelativePath(vDir.getPath(), relativePath, '/');
                    if (relativePath == null) {
                        continue;
                    }
                }

                // only suggested non-existent folders
                if (vDir.findFileByRelativePath(relativePath) != null) {
                    continue;
                }

                ContentFolderTypeProvider rootType = variant.getRootType();

                Image icon = rootType == null ? null : rootType.getIcon();
                if (icon == null) {
                    icon = variant.getIcon();
                }
                if (icon == null) {
                    icon = PlatformIconGroup.nodesFolder();
                }

                CompletionItem completionItem = new CompletionItem(contributor, relativePath, icon, rootType);
                if (!variants.contains(completionItem)) {
                    variants.add(completionItem);
                }
            }
        });

        variants.sort((o1, o2) -> {
            int result = StringUtil.naturalCompare(o1.myContributor.getDescription().get(), o2.myContributor.getDescription().get());
            if (result != 0) {
                return result;
            }
            return StringUtil.naturalCompare(o1.myRelativePath, o2.myRelativePath);
        });

        return variants;
    }

    @RequiredUIAccess
    private static @Nullable List<PsiElement> createDirectories(
        List<Pair<String, @Nullable ContentFolderTypeProvider>> toCreate,
        CreateDirectoryOrPackageHandler validator
    ) {
        List<PsiElement> createdDirectories = new ArrayList<>(toCreate.size());

        // first, check that we can create all requested directories
        for (Pair<String, @Nullable ContentFolderTypeProvider> dir : toCreate) {
            if (dir.getFirst().isEmpty() || !validator.checkInput(dir.getFirst())) {
                return null;
            }
        }

        List<Pair<PsiFileSystemItem, ContentFolderTypeProvider>> toMarkAsRoots = new ArrayList<>(toCreate.size());

        // now create directories one by one
        for (Pair<String, @Nullable ContentFolderTypeProvider> dir : toCreate) {
            // this call creates a directory
            if (!validator.canClose(dir.getFirst())) {
                return null;
            }
            PsiFileSystemItem element = validator.getCreatedElement();
            if (element == null) {
                continue;
            }

            createdDirectories.add(element);

            // collect folders to mark as source folders later
            ContentFolderTypeProvider rootType = dir.getSecond();
            if (rootType != null) {
                toMarkAsRoots.add(Pair.create(element, rootType));
            }
        }

        if (!toMarkAsRoots.isEmpty()) {
            Project project = toMarkAsRoots.get(0).getFirst().getProject();
            ProjectFileIndex index = ProjectFileIndex.getInstance(project);

            WriteAction.run(() -> ProjectRootManager.getInstance(project).mergeRootsChangesDuring(() -> {
                for (Pair<PsiFileSystemItem, ContentFolderTypeProvider> each : toMarkAsRoots) {
                    VirtualFile file = each.getFirst().getVirtualFile();
                    ContentFolderTypeProvider rootType = each.getSecond();

                    // make sure we have a content root for this directory and it's not yet registered as source folder
                    Module module = index.getModuleForFile(file);
                    if (module == null || index.getContentRootForFile(file) == null || index.isInSourceContent(file)) {
                        continue;
                    }

                    ModifiableRootModel model = ModuleRootManager.getInstance(module).getModifiableModel();
                    ContentEntry entry = MarkRootAction.findContentEntry(model, file);
                    if (entry != null) {
                        entry.addFolder(file, rootType);
                        model.commit();
                    }
                    else {
                        model.dispose();
                    }
                }
            }));
        }

        return createdDirectories;
    }

    @Override
    @RequiredUIAccess
    public void update(AnActionEvent event) {
        Presentation presentation = event.getPresentation();

        Project project = event.getData(Project.KEY);
        IdeView view = event.getData(IdeView.KEY);
        if (project == null || view == null) {
            presentation.setEnabledAndVisible(false);
            return;
        }

        PsiDirectory[] directories = view.getDirectories();
        if (directories.length == 0) {
            presentation.setEnabledAndVisible(false);
            return;
        }

        presentation.setEnabledAndVisible(true);

        // is more that one directories not show package support
        if (directories.length > 1) {
            presentation.setText(CreateDirectoryOrPackageType.Directory.getName());
            presentation.setIcon(PlatformIconGroup.nodesTreeclosed());
        }
        else {
            Trinity<ContentFolderTypeProvider, PsiDirectory, CreateDirectoryOrPackageType> info = getInfo(directories[0]);

            presentation.setText(info.getThird().getName());

            ContentFolderTypeProvider first = info.getFirst();
            Image childIcon;
            if (first == null) {
                childIcon = PlatformIconGroup.nodesTreeclosed();
            }
            else {
                childIcon = first.getChildPackageIcon() == null ? first.getChildDirectoryIcon() : first.getChildPackageIcon();
            }
            presentation.setIcon(childIcon);
        }
    }

    
    @RequiredUIAccess
    private static Trinity<ContentFolderTypeProvider, PsiDirectory, CreateDirectoryOrPackageType> getInfo(PsiDirectory d) {
        Project project = d.getProject();
        ProjectFileIndex projectFileIndex = ProjectFileIndex.getInstance(project);

        Module moduleForPsiElement = d.getModule();
        if (moduleForPsiElement != null) {
            ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(moduleForPsiElement);
            ExtensionPoint<PsiPackageSupportProvider> extensionPoint =
                project.getApplication().getExtensionPoint(PsiPackageSupportProvider.class);
            boolean isPackageSupported = false;
            for (ModuleExtension moduleExtension : moduleRootManager.getExtensions()) {
                isPackageSupported = extensionPoint.anyMatchSafe(supportProvider -> supportProvider.isSupported(moduleExtension));
                if (isPackageSupported) {
                    break;
                }
            }

            if (isPackageSupported) {
                ContentFolderTypeProvider contentFolderTypeForFile = projectFileIndex.getContentFolderTypeForFile(d.getVirtualFile());
                if (contentFolderTypeForFile != null) {
                    Image childPackageIcon = contentFolderTypeForFile.getChildPackageIcon();
                    return Trinity.create(
                        contentFolderTypeForFile,
                        d,
                        childPackageIcon != null ? CreateDirectoryOrPackageType.Package : CreateDirectoryOrPackageType.Directory
                    );
                }
            }
        }

        return Trinity.create(null, d, CreateDirectoryOrPackageType.Directory);
    }

    /**
     * An element of the suggestions list: a suggested directory or the title of a contributor's group.
     */
    private sealed interface DirectoryListElement permits CompletionItem, ContributorSeparator {
    }

    private record ContributorSeparator(CreateDirectoryCompletionContributor contributor) implements DirectoryListElement {
    }

    protected static final class CompletionItem implements DirectoryListElement {
        private final CreateDirectoryCompletionContributor myContributor;

        private final String myRelativePath;
        private final @Nullable ContentFolderTypeProvider myRootType;

        private final String myDisplayText;
        private final Image myIcon;

        private CompletionItem(
            CreateDirectoryCompletionContributor contributor,
            String relativePath,
            Image icon,
            @Nullable ContentFolderTypeProvider rootType
        ) {
            myContributor = contributor;

            myRelativePath = relativePath;
            myRootType = rootType;

            myDisplayText = FileUtil.toSystemDependentName(relativePath);
            myIcon = icon;
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            CompletionItem that = (CompletionItem) o;
            return myContributor.equals(that.myContributor) &&
                myRelativePath.equals(that.myRelativePath) &&
                Objects.equals(myRootType, that.myRootType) &&
                myDisplayText.equals(that.myDisplayText) &&
                Objects.equals(myIcon, that.myIcon);
        }

        @Override
        public int hashCode() {
            return Objects.hash(myContributor, myRelativePath, myRootType, myDisplayText, myIcon);
        }
    }

    private static final class DirectoriesWithCompletionPopupPanel extends NewItemWithTemplatesPopupPanel<DirectoryListElement> {
        private static final TextAttribute MATCHED =
            new TextAttribute(TextAttribute.STYLE_PLAIN, ComponentColors.SEARCH_MATCH_FOREGROUND, ComponentColors.SEARCH_MATCH_BACKGROUND);

        private @Nullable MinusculeMatcher myCurrentMatcher = null;
        private boolean myLocked = false;

        @RequiredUIAccess
        private DirectoriesWithCompletionPopupPanel(List<CompletionItem> items) {
            super(withSeparators(items), (presentation, item) -> {
            });
            myTemplatesList.setRenderer(this::render);
            myTemplatesList.isSeparator(element -> element instanceof ContributorSeparator);
            myTemplatesList.setSeparatorCaption(element -> element instanceof ContributorSeparator separator
                ? separator.contributor().getDescription()
                : LocalizeValue.empty());

            myTemplatesList.addValueListener(e -> {
                if (e.getValue() instanceof CompletionItem selected) {
                    myLocked = true;
                    try {
                        myTextField.setValue(selected.myDisplayText);
                    }
                    finally {
                        myLocked = false;
                    }
                }
            });
            myTextField.addValueListener(e -> {
                if (!myLocked) {
                    String input = StringUtil.notNullize(myTextField.getValue());
                    MinusculeMatcher matcher = NameUtil.buildMatcher("*" + input).build();
                    myCurrentMatcher = matcher;

                    List<CompletionItem> filtered = new ArrayList<>();
                    for (CompletionItem item : items) {
                        if (matcher.matches(item.myDisplayText)) {
                            filtered.add(item);
                        }
                    }

                    updateTemplatesList(withSeparators(filtered));
                    setTemplatesListVisible(!filtered.isEmpty());
                }
            });
            setTemplatesListVisible(!items.isEmpty());
        }

        private static List<DirectoryListElement> withSeparators(List<CompletionItem> items) {
            List<DirectoryListElement> elements = new ArrayList<>(items.size());
            CreateDirectoryCompletionContributor contributor = null;
            for (CompletionItem item : items) {
                if (item.myContributor != contributor) {
                    contributor = item.myContributor;
                    elements.add(new ContributorSeparator(contributor));
                }
                elements.add(item);
            }
            return elements;
        }

        List<CompletionItem> getSelectedItems() {
            return myTemplatesList.getValue() instanceof CompletionItem item ? List.of(item) : List.of();
        }

        @RequiredUIAccess
        private void render(TextItemPresentation presentation, consulo.ui.RenderItem<DirectoryListElement> item) {
            DirectoryListElement value = item.getValue();
            if (value instanceof ContributorSeparator separator) {
                presentation.append(separator.contributor().getDescription());
                return;
            }
            if (!(value instanceof CompletionItem completionItem)) {
                return;
            }

            presentation.withIcon(completionItem.myIcon);

            String text = completionItem.myDisplayText;
            MinusculeMatcher matcher = myCurrentMatcher;
            FList<MatcherTextRange> ranges = matcher == null ? null : matcher.matchingFragments(text);
            if (ranges == null || ranges.isEmpty()) {
                presentation.append(text);
                return;
            }

            int offset = 0;
            for (MatcherTextRange range : ranges) {
                if (range.getStartOffset() > offset) {
                    presentation.append(text.substring(offset, range.getStartOffset()));
                }
                presentation.append(text.substring(range.getStartOffset(), range.getEndOffset()), MATCHED);
                offset = range.getEndOffset();
            }
            if (offset < text.length()) {
                presentation.append(text.substring(offset));
            }
        }
    }
}
