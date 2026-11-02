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
package consulo.language.spellchecker.editor.dictionary;


import consulo.language.spellchecker.editor.SpellCheckerManager;
import consulo.language.spellchecker.editor.SpellcheckingUtil;
import consulo.annotation.access.RequiredReadAction;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.document.util.TextRange;
import consulo.language.Language;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.language.spellcheker.tokenizer.TokenConsumer;
import consulo.language.spellcheker.tokenizer.splitter.SplitContext;
import consulo.language.spellcheker.tokenizer.splitter.TokenSplitter;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.util.collection.MultiMap;
import consulo.util.io.FileUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import consulo.virtualFileSystem.util.VirtualFileVisitor;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public abstract class SpellCheckerDictionaryGenerator {
    private static final Logger LOG = Logger.getInstance(SpellCheckerDictionaryGenerator.class);
    private final Set<String> globalSeenNames = new HashSet<>();
    protected final Project myProject;
    private final String myDefaultDictName;
    protected final String myDictOutputFolder;
    protected final MultiMap<String, VirtualFile> myDict2FolderMap;
    protected final Set<VirtualFile> myExcludedFolders = new HashSet<>();
    protected SpellCheckerManager mySpellCheckerManager;

    public SpellCheckerDictionaryGenerator(Project project, String dictOutputFolder, String defaultDictName) {
        myDict2FolderMap = new MultiMap<>();
        myProject = project;
        myDefaultDictName = defaultDictName;
        mySpellCheckerManager = SpellCheckerManager.getInstance(myProject);
        myDictOutputFolder = dictOutputFolder;
    }

    public void addFolder(String dictName, VirtualFile path) {
        myDict2FolderMap.putValue(dictName, path);
    }

    public void excludeFolder(VirtualFile folder) {
        myExcludedFolders.add(folder);
    }

    public void generate() {
        ProgressManager.getInstance().runProcessWithProgressSynchronously(
            () -> {
                ProgressIndicator progressIndicator = ProgressManager.getInstance().getProgressIndicator();
                // let's do result a bit more predictable

                // ruby dictionary
                generate(myDefaultDictName, progressIndicator);

                // other gem-related dictionaries in alphabet order
                List<String> dictionaries = new ArrayList<>(myDict2FolderMap.keySet());
                Collections.sort(dictionaries);

                for (String dict : dictionaries) {
                    if (myDefaultDictName.equals(dict)) {
                        continue;
                    }
                    generate(dict, progressIndicator);
                }
            },
            "Generating Dictionaries",
            false,
            myProject
        );
    }

    private void generate(String dict, ProgressIndicator progressIndicator) {
        progressIndicator.setText("Processing dictionary: " + dict);
        generateDictionary(myProject, myDict2FolderMap.get(dict), myDictOutputFolder + "/" + dict + ".dic", progressIndicator);
    }

    private void generateDictionary(
        Project project,
        Collection<VirtualFile> folderPaths,
        String outFile,
        ProgressIndicator progressIndicator
    ) {
        Set<String> seenNames = new HashSet<>();
        // Collect stuff
        for (VirtualFile folder : folderPaths) {
            progressIndicator.setText2("Scanning folder: " + folder.getPath());
            PsiManager manager = PsiManager.getInstance(project);
            processFolder(seenNames, manager, folder);
        }

        if (seenNames.isEmpty()) {
            LOG.info("  No new words was found.");
            return;
        }

        StringBuilder builder = new StringBuilder();
        // Sort names
        List<String> names = new ArrayList<>(seenNames);
        Collections.sort(names);
        for (String name : names) {
            if (builder.length() > 0) {
                builder.append("\n");
            }
            builder.append(name);
        }
        try {
            File dictionaryFile = new File(outFile);
            FileUtil.createIfDoesntExist(dictionaryFile);
            FileWriter writer = new FileWriter(dictionaryFile.getPath());
            try {
                writer.write(builder.toString());
            }
            finally {
                writer.close();
            }
        }
        catch (IOException e) {
            LOG.error(e);
        }
    }

    protected void processFolder(final Set<String> seenNames, final PsiManager manager, VirtualFile folder) {
        VirtualFileUtil.visitChildrenRecursively(folder, new VirtualFileVisitor() {
            @Override
            @RequiredReadAction
            public boolean visitFile(VirtualFile file) {
                if (myExcludedFolders.contains(file)) {
                    return false;
                }
                if (!file.isDirectory()) {
                    PsiFile psiFile = manager.findFile(file);
                    if (psiFile != null) {
                        processFile(psiFile, seenNames);
                    }
                }
                return true;
            }
        });
    }

    protected abstract void processFile(PsiFile file, Set<String> seenNames);

    /**
     * @return true if the word is a keyword of the language; keywords are not written to dictionaries
     */
    protected boolean isKeyword(String word, Language language) {
        return false;
    }

    @RequiredReadAction
    protected void process(PsiElement element, Set<String> seenNames) {
        int endOffset = element.getTextRange().getEndOffset();

        // collect leafs  (spell checker inspection works with leafs)
        List<PsiElement> leafs = new ArrayList<>();
        if (element.getChildren().length == 0) {
            // if no children - it is a leaf!
            leafs.add(element);
        }
        else {
            // else collect leafs under given element
            PsiElement currentLeaf = PsiTreeUtil.firstChild(element);
            while (currentLeaf != null && currentLeaf.getTextRange().getEndOffset() <= endOffset) {
                leafs.add(currentLeaf);
                currentLeaf = PsiTreeUtil.nextLeaf(currentLeaf);
            }
        }

        for (PsiElement leaf : leafs) {
            processLeafsNames(leaf, seenNames);
        }
    }

    @RequiredReadAction
    protected void processLeafsNames(PsiElement leafElement, final Set<String> seenNames) {
        final Language language = leafElement.getLanguage();
        SpellcheckingUtil.tokenize(
            leafElement,
            new TokenConsumer() {
                @Override
                @RequiredReadAction
                public void consumeToken(
                    PsiElement element,
                    String text,
                    boolean useRename,
                    int offset,
                    TextRange rangeToCheck,
                    TokenSplitter splitter
                ) {
                    splitter.split(
                        SplitContext.of(
                            text,
                            textRange -> {
                                String word = textRange.substring(text);
                                addSeenWord(seenNames, word, language);
                            }
                        ),
                        rangeToCheck
                    );
                }
            }
        );
    }

    protected void addSeenWord(Set<String> seenNames, String word, Language language) {
        String lowerWord = word.toLowerCase();
        if (globalSeenNames.contains(lowerWord)) {
            return;
        }

        if (isKeyword(word, language)) {
            return;
        }

        globalSeenNames.add(lowerWord);
        if (mySpellCheckerManager.hasProblem(lowerWord)) {
            seenNames.add(lowerWord);
        }
    }
}
