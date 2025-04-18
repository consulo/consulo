/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package consulo.ide.impl.idea.codeInsight.daemon.impl;

import consulo.annotation.component.ServiceImpl;
import consulo.codeEditor.Editor;
import consulo.document.Document;
import consulo.util.collection.ArrayUtil;
import consulo.language.editor.Pass;
import consulo.language.editor.impl.highlight.*;
import consulo.language.editor.impl.internal.daemon.DaemonCodeAnalyzerEx;
import consulo.language.editor.impl.internal.daemon.FileStatusMapImpl;
import consulo.language.editor.impl.internal.highlight.DefaultHighlightInfoProcessor;
import consulo.language.psi.PsiCompiledElement;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiUtilCore;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.util.collection.primitive.ints.IntList;
import consulo.util.collection.primitive.ints.IntLists;
import gnu.trove.TIntHashSet;
import gnu.trove.TIntObjectHashMap;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.*;

/**
 * @author anna
 * @since 2006-04-19
 */
@Singleton
@ServiceImpl
public class TextEditorHighlightingPassManagerImpl extends TextEditorHighlightingPassManager {
    private static class PassConfig {
        private final TextEditorHighlightingPassFactory passFactory;
        private final int[] startingPredecessorIds;
        private final int[] completionPredecessorIds;

        private PassConfig(
            @Nonnull TextEditorHighlightingPassFactory passFactory,
            @Nonnull int[] completionPredecessorIds,
            @Nonnull int[] startingPredecessorIds
        ) {
            this.completionPredecessorIds = completionPredecessorIds;
            this.startingPredecessorIds = startingPredecessorIds;
            this.passFactory = passFactory;
        }
    }

    class RegistrarImpl implements TextEditorHighlightingPassFactory.Registrar {
        @Override
        public int registerTextEditorHighlightingPass(
            @Nonnull TextEditorHighlightingPassFactory factory,
            @Nullable int[] runAfterCompletionOf,
            @Nullable int[] runAfterOfStartingOf,
            boolean runIntentionsPassAfter,
            int forcedPassId
        ) {
            assert !checkedForCycles;
            PassConfig info = new PassConfig(
                factory,
                runAfterCompletionOf == null || runAfterCompletionOf.length == 0 ? ArrayUtil.EMPTY_INT_ARRAY : runAfterCompletionOf,
                runAfterOfStartingOf == null || runAfterOfStartingOf.length == 0 ? ArrayUtil.EMPTY_INT_ARRAY : runAfterOfStartingOf
            );
            int passId = forcedPassId == -1 ? nextAvailableId++ : forcedPassId;
            PassConfig registered = myRegisteredPassFactories.get(passId);
            assert registered == null : "Pass id " + passId + " has already been registered in: " + registered.passFactory;
            myRegisteredPassFactories.put(passId, info);
            if (factory instanceof DirtyScopeTrackingHighlightingPassFactory dirtyScopeTrackingHighlightingPassFactory) {
                myDirtyScopeTrackingFactories.add(dirtyScopeTrackingHighlightingPassFactory);
            }
            return passId;
        }
    }

    private static final Logger LOG = Logger.getInstance(TextEditorHighlightingPassManagerImpl.class);

    private final TIntObjectHashMap<PassConfig> myRegisteredPassFactories = new TIntObjectHashMap<>();
    private final List<DirtyScopeTrackingHighlightingPassFactory> myDirtyScopeTrackingFactories = new ArrayList<>();
    private int nextAvailableId = Pass.LAST_PASS + 1;
    private boolean checkedForCycles;
    private final Project myProject;

    @Inject
    public TextEditorHighlightingPassManagerImpl(Project project) {
        myProject = project;

        if (myProject.isDefault()) {
            return;
        }

        RegistrarImpl impl = new RegistrarImpl();

        for (TextEditorHighlightingPassFactory factory : TextEditorHighlightingPassFactory.EP_NAME.getExtensionList(myProject)) {
            int old = myRegisteredPassFactories.size();
            factory.register(impl);
            if (old == myRegisteredPassFactories.size()) {
                LOG.error(factory.getClass().getName() + " is not registered to manager");
            }
        }
    }

    @Override
    @Nonnull
    @SuppressWarnings("unchecked")
    public List<TextEditorHighlightingPass> instantiatePasses(
        @Nonnull PsiFile psiFile,
        @Nonnull Editor editor,
        @Nonnull int[] passesToIgnore
    ) {
        synchronized (this) {
            if (!checkedForCycles) {
                checkedForCycles = true;
                checkForCycles();
            }
        }
        PsiDocumentManager documentManager = PsiDocumentManager.getInstance(myProject);
        Document document = editor.getDocument();
        PsiFile fileFromDoc = documentManager.getPsiFile(document);
        if (!(fileFromDoc instanceof PsiCompiledElement)) {
            assert fileFromDoc == psiFile : "Files are different: " + psiFile + ";" + fileFromDoc;
            Document documentFromFile = documentManager.getDocument(psiFile);
            assert documentFromFile == document : "Documents are different. Doc: " + document +
                "; Doc from file: " + documentFromFile +
                "; File: " + psiFile +
                "; Virtual file: " + PsiUtilCore.getVirtualFile(psiFile);
        }
        TIntObjectHashMap<TextEditorHighlightingPass> id2Pass = new TIntObjectHashMap<>();
        IntList passesRefusedToCreate = IntLists.newArrayList();
        myRegisteredPassFactories.forEachKey(passId -> {
            if (ArrayUtil.find(passesToIgnore, passId) != -1) {
                return true;
            }
            PassConfig passConfig = myRegisteredPassFactories.get(passId);
            TextEditorHighlightingPassFactory factory = passConfig.passFactory;
            TextEditorHighlightingPass pass = factory.createHighlightingPass(psiFile, editor);

            if (pass == null) {
                passesRefusedToCreate.add(passId);
            }
            else {
                // init with editor's colors scheme
                pass.setColorsScheme(editor.getColorsScheme());

                IntList ids = IntLists.newArrayList(passConfig.completionPredecessorIds.length);
                for (int id : passConfig.completionPredecessorIds) {
                    if (myRegisteredPassFactories.containsKey(id)) {
                        ids.add(id);
                    }
                }
                pass.setCompletionPredecessorIds(ids.isEmpty() ? ArrayUtil.EMPTY_INT_ARRAY : ids.toArray());
                ids = IntLists.newArrayList(passConfig.startingPredecessorIds.length);
                for (int id : passConfig.startingPredecessorIds) {
                    if (myRegisteredPassFactories.containsKey(id)) {
                        ids.add(id);
                    }
                }
                pass.setStartingPredecessorIds(ids.isEmpty() ? ArrayUtil.EMPTY_INT_ARRAY : ids.toArray());
                pass.setId(passId);
                id2Pass.put(passId, pass);
            }
            return true;
        });

        DaemonCodeAnalyzerEx daemonCodeAnalyzer = DaemonCodeAnalyzerEx.getInstanceEx(myProject);
        FileStatusMapImpl statusMap = daemonCodeAnalyzer.getFileStatusMap();
        passesRefusedToCreate.forEach(passId -> statusMap.markFileUpToDate(document, passId));

        return (List)Arrays.asList(id2Pass.getValues());
    }

    @Nonnull
    @Override
    public List<TextEditorHighlightingPass> instantiateMainPasses(@Nonnull PsiFile psiFile, @Nonnull Document document) {
        return instantiateMainPasses(psiFile, document, new DefaultHighlightInfoProcessor());
    }

    @Nonnull
    @Override
    public List<TextEditorHighlightingPass> instantiateMainPasses(
        @Nonnull PsiFile psiFile,
        @Nonnull Document document,
        @Nonnull HighlightInfoProcessor highlightInfoProcessor
    ) {
        Set<TextEditorHighlightingPass> ids = new HashSet<>();
        myRegisteredPassFactories.forEachKey(passId -> {
            PassConfig passConfig = myRegisteredPassFactories.get(passId);
            TextEditorHighlightingPassFactory factory = passConfig.passFactory;
            if (factory instanceof MainHighlightingPassFactory mainHighlightingPassFactory) {
                TextEditorHighlightingPass pass =
                    mainHighlightingPassFactory.createMainHighlightingPass(psiFile, document, highlightInfoProcessor);
                if (pass != null) {
                    ids.add(pass);
                    pass.setId(passId);
                }
            }
            return true;
        });
        return new ArrayList<>(ids);
    }

    private void checkForCycles() {
        TIntObjectHashMap<TIntHashSet> transitivePredecessors = new TIntObjectHashMap<>();

        myRegisteredPassFactories.forEachEntry((passId, config) -> {
            TIntHashSet allPredecessors = new TIntHashSet(config.completionPredecessorIds);
            allPredecessors.addAll(config.startingPredecessorIds);
            transitivePredecessors.put(passId, allPredecessors);
            allPredecessors.forEach(predecessorId -> {
                PassConfig predecessor = myRegisteredPassFactories.get(predecessorId);
                if (predecessor == null) {
                    return true;
                }
                TIntHashSet transitives = transitivePredecessors.get(predecessorId);
                if (transitives == null) {
                    transitives = new TIntHashSet();
                    transitivePredecessors.put(predecessorId, transitives);
                }
                transitives.addAll(predecessor.completionPredecessorIds);
                transitives.addAll(predecessor.startingPredecessorIds);
                return true;
            });
            return true;
        });
        transitivePredecessors.forEachKey(passId -> {
            if (transitivePredecessors.get(passId).contains(passId)) {
                throw new IllegalArgumentException("There is a cycle introduced involving pass " + myRegisteredPassFactories.get(passId).passFactory);
            }
            return true;
        });
    }

    @Nonnull
    @Override
    public List<DirtyScopeTrackingHighlightingPassFactory> getDirtyScopeTrackingFactories() {
        return myDirtyScopeTrackingFactories;
    }
}
