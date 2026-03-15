/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.versionControlSystem.log.impl.internal.ui.action;

import consulo.application.progress.ProgressManager;
import consulo.language.editor.completion.CompletionParameters;
import consulo.language.editor.completion.CompletionResultSet;
import consulo.language.editor.completion.lookup.InsertHandler;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.LookupElementBuilder;
import consulo.language.editor.ui.awt.DefaultTextCompletionValueDescriptor;
import consulo.language.editor.ui.awt.ValuesCompletionProvider;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.ex.awt.JBLabel;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.event.JBPopupListener;
import consulo.ui.ex.popup.event.LightweightWindowEvent;
import consulo.ui.image.ImageEffects;
import consulo.util.collection.ContainerUtil;
import consulo.versionControlSystem.log.VcsLogRefs;
import consulo.versionControlSystem.log.VcsRef;
import consulo.versionControlSystem.log.impl.internal.ui.VcsLogGraphTable;
import consulo.versionControlSystem.log.ui.VcsLogColorManager;
import consulo.versionControlSystem.util.VcsUtil;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GoToHashOrRefPopup {
    private static final Logger LOG = Logger.getInstance(GoToHashOrRefPopup.class);

    
    private final Project myProject;
    
    private final TextFieldWithProgress myTextField;
    
    private final Function<String, Future> myOnSelectedHash;
    
    private final Function<VcsRef, Future> myOnSelectedRef;
    
    private final JBPopup myPopup;
    @Nullable
    private Future myFuture;
    @Nullable
    private VcsRef mySelectedRef;

    public GoToHashOrRefPopup(
        Project project,
        VcsLogRefs variants,
        Collection<VirtualFile> roots,
        Function<String, Future> onSelectedHash,
        Function<VcsRef, Future> onSelectedRef,
        VcsLogColorManager colorManager,
        Comparator<VcsRef> comparator
    ) {
        myProject = project;
        myOnSelectedHash = onSelectedHash;
        myOnSelectedRef = onSelectedRef;
        myTextField = new TextFieldWithProgress(
            myProject,
            new VcsRefCompletionProvider(myProject, variants, roots, colorManager, comparator)
        ) {
            @Override
            public void onOk() {
                if (myFuture == null) {
                    Future future = mySelectedRef == null || !mySelectedRef.getName().equals(getText().trim())
                        ? myOnSelectedHash.apply(getText().trim())
                        : myOnSelectedRef.apply(mySelectedRef);
                    myFuture = future;
                    showProgress();
                    myProject.getApplication().executeOnPooledThread(() -> {
                        try {
                            future.get();
                            okPopup();
                        }
                        catch (CancellationException | InterruptedException ex) {
                            cancelPopup();
                        }
                        catch (ExecutionException ex) {
                            LOG.error(ex);
                            cancelPopup();
                        }
                    });
                }
            }
        };
        myTextField.setAlignmentX(Component.LEFT_ALIGNMENT);

        JBLabel label = new JBLabel("Enter hash or branch/tag name:");
        label.setFont(UIUtil.getLabelFont().deriveFont(Font.BOLD));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel panel = new JPanel();
        BoxLayout layout = new BoxLayout(panel, BoxLayout.PAGE_AXIS);
        panel.setLayout(layout);
        panel.add(label);
        panel.add(myTextField);
        panel.setBorder(new EmptyBorder(2, 2, 2, 2));

        myPopup = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(panel, myTextField.getPreferableFocusComponent())
            .setCancelOnClickOutside(true)
            .setCancelOnWindowDeactivation(true)
            .setCancelKeyEnabled(true)
            .setRequestFocus(true)
            .createPopup();
        myPopup.addListener(new JBPopupListener() {
            @Override
            public void onClosed(LightweightWindowEvent event) {
                if (!event.isOk() && myFuture != null) {
                    myFuture.cancel(true);
                }
                myFuture = null;
                myTextField.hideProgress();
            }
        });
    }

    private void cancelPopup() {
        myProject.getApplication().invokeLater(myPopup::cancel);
    }

    private void okPopup() {
        myProject.getApplication().invokeLater(() -> myPopup.closeOk(null));
    }

    public void show(JComponent anchor) {
        myPopup.showInCenterOf(anchor);
    }

    private class VcsRefCompletionProvider extends ValuesCompletionProvider<VcsRef> {
        private static final int TIMEOUT = 100;
        
        private final VcsLogRefs myRefs;
        
        private final Collection<VirtualFile> myRoots;

        public VcsRefCompletionProvider(
            Project project,
            VcsLogRefs refs,
            Collection<VirtualFile> roots,
            VcsLogColorManager colorManager,
            Comparator<VcsRef> comparator
        ) {
            super(new VcsRefDescriptor(project, colorManager, comparator, roots), List.of());
            myRefs = refs;
            myRoots = roots;
        }

        @Override
        public void fillCompletionVariants(
            CompletionParameters parameters,
            String prefix,
            CompletionResultSet result
        ) {
            addValues(result, filterAndSort(result, myRefs.getBranches().stream()));

            Future<List<VcsRef>> future = myProject.getApplication()
                .executeOnPooledThread(() -> filterAndSort(result, myRefs.stream().filter(ref -> !ref.getType().isBranch())));
            while (true) {
                try {
                    List<VcsRef> tags = future.get(TIMEOUT, TimeUnit.MILLISECONDS);
                    if (tags != null) {
                        addValues(result, tags);
                        break;
                    }
                }
                catch (InterruptedException | CancellationException e) {
                    break;
                }
                catch (TimeoutException ignored) {
                }
                catch (ExecutionException e) {
                    LOG.error(e);
                    break;
                }
                ProgressManager.checkCanceled();
            }
            result.stopHere();
        }

        public void addValues(CompletionResultSet result, Collection<? extends VcsRef> values) {
            for (VcsRef completionVariant : values) {
                result.addElement(installInsertHandler(myDescriptor.createLookupBuilder(completionVariant)));
            }
        }

        
        private List<VcsRef> filterAndSort(CompletionResultSet result, Stream<VcsRef> stream) {
            return ContainerUtil.sorted(stream.filter(ref -> myRoots.contains(ref.getRoot())
                && result.getPrefixMatcher().prefixMatches(ref.getName())).collect(Collectors.toList()), myDescriptor);
        }
    }

    private class VcsRefDescriptor extends DefaultTextCompletionValueDescriptor<VcsRef> {
        
        private final Project myProject;
        
        private final VcsLogColorManager myColorManager;
        
        private final Comparator<VcsRef> myReferenceComparator;
        
        private final Map<VirtualFile, String> myCachedRootNames = new HashMap<>();

        private VcsRefDescriptor(
            Project project,
            VcsLogColorManager manager,
            Comparator<VcsRef> comparator,
            Collection<VirtualFile> roots
        ) {
            myProject = project;
            myColorManager = manager;
            myReferenceComparator = comparator;

            for (VirtualFile root : roots) {
                String text = VcsUtil.getShortVcsRootName(myProject, root);
                myCachedRootNames.put(root, text);
            }
        }

        
        @Override
        public LookupElementBuilder createLookupBuilder(VcsRef item) {
            LookupElementBuilder lookupBuilder = super.createLookupBuilder(item);
            if (myColorManager.isMultipleRoots()) {
                lookupBuilder = lookupBuilder.withTypeText(
                    getTypeText(item),
                    ImageEffects.colorFilled(
                        15,
                        15,
                        TargetAWT.from(VcsLogGraphTable.getRootBackgroundColor(item.getRoot(), myColorManager))
                    ),
                    true
                );
            }
            return lookupBuilder;
        }

        
        @Override
        public String getLookupString(VcsRef item) {
            return item.getName();
        }

        @Nullable
        @Override
        protected String getTailText(VcsRef item) {
            if (!myColorManager.isMultipleRoots()) {
                return null;
            }
            return "";
        }

        @Nullable
        @Override
        protected String getTypeText(VcsRef item) {
            if (!myColorManager.isMultipleRoots()) {
                return null;
            }
            String text = myCachedRootNames.get(item.getRoot());
            if (text == null) {
                return VcsUtil.getShortVcsRootName(myProject, item.getRoot());
            }
            return text;
        }

        @Override
        public int compare(VcsRef item1, VcsRef item2) {
            return myReferenceComparator.compare(item1, item2);
        }

        @Nullable
        @Override
        protected InsertHandler<LookupElement> createInsertHandler(VcsRef item) {
            return (context, item1) -> {
                mySelectedRef = (VcsRef) item1.getObject();
                // handleInsert is called in the middle of some other code that works with editor
                // (see CodeCompletionHandlerBase.insertItem)
                // for example, scrolls editor
                // problem is that in onOk we make text field not editable
                // by some reason this is done by disposing its editor and creating a new one
                // so editor gets disposed here and CodeCompletionHandlerBase can not finish doing whatever it is doing with it
                // I counter this by invoking onOk in invokeLater
                myProject.getApplication().invokeLater(myTextField::onOk);
            };
        }
    }
}
