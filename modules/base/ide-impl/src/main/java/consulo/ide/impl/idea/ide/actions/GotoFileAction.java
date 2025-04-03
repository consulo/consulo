/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

import consulo.application.ApplicationManager;
import consulo.application.dumb.DumbAware;
import consulo.externalService.statistic.FeatureUsageTracker;
import consulo.fileEditor.impl.internal.OpenFileDescriptorImpl;
import consulo.ide.impl.idea.ide.actions.searcheverywhere.FileSearchEverywhereContributor;
import consulo.ide.impl.idea.ide.util.gotoByName.ChooseByNameFilter;
import consulo.ide.impl.idea.ide.util.gotoByName.ChooseByNamePopup;
import consulo.ide.impl.idea.ide.util.gotoByName.GotoFileConfiguration;
import consulo.ide.impl.idea.ide.util.gotoByName.GotoFileModel;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.language.file.FileTypeManager;
import consulo.language.psi.PsiFile;
import consulo.navigation.Navigatable;
import consulo.ide.localize.IdeLocalize;
import consulo.project.Project;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.image.Image;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.virtualFileSystem.fileType.UnknownFileType;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * "Go to | File" action implementation.
 *
 * @author Eugene Belyaev
 * @author Constantine.Plotnikov
 */
public class GotoFileAction extends GotoActionBase implements DumbAware {
    public static final String ID = "GotoFile";

    @Override
    public void actionPerformed(@Nonnull AnActionEvent e) {
        showInSearchEverywherePopup(FileSearchEverywhereContributor.class.getSimpleName(), e, true, true);
    }

    @Override
    public void gotoActionPerformed(@Nonnull AnActionEvent e) {
        final Project project = e.getData(Project.KEY);
        if (project == null) {
            return;
        }

        FeatureUsageTracker.getInstance().triggerFeatureUsed("navigation.popup.file");

        final GotoFileModel gotoFileModel = new GotoFileModel(project);
        GotoActionCallback<FileType> callback = new GotoActionCallback<>() {
            @Override
            protected ChooseByNameFilter<FileType> createFilter(@Nonnull ChooseByNamePopup popup) {
                return new GotoFileFilter(popup, gotoFileModel, project);
            }

            @Override
            public void elementChosen(final ChooseByNamePopup popup, final Object element) {
                if (element == null) {
                    return;
                }
                ApplicationManager.getApplication().assertIsDispatchThread();
                Navigatable n = (Navigatable)element;
                //this is for better cursor position
                if (element instanceof PsiFile) {
                    VirtualFile file = ((PsiFile)element).getVirtualFile();
                    if (file == null) {
                        return;
                    }
                    OpenFileDescriptorImpl descriptor =
                        new OpenFileDescriptorImpl(project, file, popup.getLinePosition(), popup.getColumnPosition());
                    n = descriptor.setUseCurrentWindow(popup.isOpenInCurrentWindowRequested());
                }

                if (n.canNavigate()) {
                    n.navigate(true);
                }
            }
        };
        showNavigationPopup(
            e,
            gotoFileModel,
            callback,
            IdeLocalize.goToFileToolwindowTitle().get(),
            true,
            true
        );
    }

    protected static class GotoFileFilter extends ChooseByNameFilter<FileType> {
        GotoFileFilter(final ChooseByNamePopup popup, GotoFileModel model, final Project project) {
            super(popup, model, GotoFileConfiguration.getInstance(project), project);
        }

        @Override
        @Nonnull
        protected List<FileType> getAllFilterValues() {
            List<FileType> elements = new ArrayList<>();
            ContainerUtil.addAll(elements, FileTypeManager.getInstance().getRegisteredFileTypes());
            Collections.sort(elements, FileTypeComparator.INSTANCE);
            return elements;
        }

        @Override
        protected String textForFilterValue(@Nonnull FileType value) {
            return value.getName();
        }

        @Override
        protected Image iconForFilterValue(@Nonnull FileType value) {
            return value.getIcon();
        }
    }

    /**
     * A file type comparator. The comparison rules are applied in the following order.
     * <ol>
     * <li>Unknown file type is greatest.</li>
     * <li>Text files are less then binary ones.</li>
     * <li>File type with greater name is greater (case is ignored).</li>
     * </ol>
     */
    public static class FileTypeComparator implements Comparator<FileType> {
        /**
         * an instance of comparator
         */
        public static final Comparator<FileType> INSTANCE = new FileTypeComparator();

        /**
         * {@inheritDoc}
         */
        @Override
        public int compare(final FileType o1, final FileType o2) {
            if (o1 == o2) {
                return 0;
            }
            if (o1 == UnknownFileType.INSTANCE) {
                return 1;
            }
            if (o2 == UnknownFileType.INSTANCE) {
                return -1;
            }
            if (o1.isBinary() && !o2.isBinary()) {
                return 1;
            }
            if (!o1.isBinary() && o2.isBinary()) {
                return -1;
            }
            return o1.getName().compareToIgnoreCase(o2.getName());
        }
    }
}
