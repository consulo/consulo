// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.spellchecker.editor.impl.quickfix;

import consulo.dataContext.DataManager;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.language.editor.inspection.ProblemDescriptorUtil;
import consulo.language.editor.intention.LowPriorityAction;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.spellchecker.editor.SpellCheckerManager;
import consulo.language.spellchecker.editor.dictionary.DictionaryLayer;
import consulo.language.spellchecker.editor.dictionary.DictionaryLayersProvider;
import consulo.language.spellchecker.editor.localize.SpellCheckerLocalize;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.image.Image;
import consulo.undoRedo.CommandProcessor;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class SaveTo implements SpellCheckerQuickFix, LowPriorityAction {
    private @Nullable DictionaryLayer myLayer = null;
    private @Nullable String myWord;

    public SaveTo(DictionaryLayer layer) {
        myLayer = layer;
    }

    public SaveTo(String word) {
        myWord = word;
    }

    public SaveTo(String word, @Nullable DictionaryLayer layer) {
        myWord = word;
        myLayer = layer;
    }

    @Override
    public LocalizeValue getName() {
        return myWord != null ? SpellCheckerLocalize.save0ToDictionaryFix(myWord) : SpellCheckerLocalize.saveToDictionaryFix();
    }

    @Override
    public boolean startInWriteAction() {
        return false;
    }

    @Override
    public void applyFix(Project project, ProblemDescriptor descriptor) {
        PsiElement psiElement = descriptor.getPsiElement();
        PsiFile psiFile = psiElement.getContainingFile();
        String wordToSave = myWord != null ? myWord : ProblemDescriptorUtil.extractHighlightedText(descriptor, psiElement);
        applyFix(project, psiFile, wordToSave, myLayer);
    }

    public static void applyFix(Project project, PsiFile psiFile, String wordToSave, @Nullable DictionaryLayer layer) {
        DataManager.getInstance()
            .getDataContextFromFocusAsync()
            .onSuccess(context -> {
                if (layer == null) {
                    List<String> dictList = new ArrayList<>();
                    for (DictionaryLayer it : DictionaryLayersProvider.getAllLayers(project)) {
                        dictList.add(it.getName().get());
                    }

                    JBPopupFactory.getInstance()
                        .createPopupChooserBuilder(dictList)
                        .setTitle(SpellCheckerLocalize.selectDictionaryTitle().get())
                        .setItemChosenCallback(item -> {
                            DictionaryLayer chosenLayer = DictionaryLayersProvider.getLayer(project, item);
                            if (chosenLayer == null) {
                                return;
                            }
                            CommandProcessor.getInstance()
                                .newCommand()
                                .project(project)
                                .name(SpellCheckerLocalize.save0ToDictionaryAction(wordToSave))
                                .run(() -> acceptWord(wordToSave, chosenLayer, psiFile));
                        })
                        .createPopup()
                        .showInBestPositionFor(context);
                }
                else {
                    acceptWord(wordToSave, layer, psiFile);
                }
            });
    }

    private static void acceptWord(String word, DictionaryLayer layer, PsiFile file) {
        Project project = file.getProject();
        VirtualFile virtualFile = file.getViewProvider().getVirtualFile();
        SpellCheckerManager.getInstance(project).acceptWordAsCorrect(word, virtualFile, layer);
    }

    @Override
    public Image getIcon(int flags) {
        return PlatformIconGroup.generalInspectionstypos();
    }

    public static LocalizeValue getFixName() {
        return SpellCheckerLocalize.saveToDictionaryFix();
    }
}
