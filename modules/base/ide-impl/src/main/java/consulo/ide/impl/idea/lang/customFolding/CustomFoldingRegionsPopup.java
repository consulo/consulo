/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.ide.impl.idea.lang.customFolding;

import consulo.ide.localize.IdeLocalize;
import consulo.language.editor.folding.FoldingDescriptor;
import consulo.codeEditor.Editor;
import consulo.codeEditor.ScrollType;
import consulo.fileEditor.history.IdeDocumentHistory;
import consulo.project.Project;
import consulo.ui.ex.popup.JBPopup;
import consulo.ide.impl.ui.impl.PopupChooserBuilder;
import consulo.language.psi.PsiElement;
import consulo.ui.ex.awt.JBList;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.util.*;

/**
 * @author Rustam Vishnyakov
 */
public class CustomFoldingRegionsPopup {
    private final 
    JBList myRegionsList;
    private final 
    JBPopup myPopup;
    private final 
    Editor myEditor;

    CustomFoldingRegionsPopup(
        Collection<FoldingDescriptor> descriptors,
        final Editor editor,
        final Project project
    ) {
        myEditor = editor;
        myRegionsList = new JBList();
        //noinspection unchecked
        myRegionsList.setModel(new MyListModel(orderByPosition(descriptors)));
        myRegionsList.setSelectedIndex(0);

        PopupChooserBuilder popupBuilder = new PopupChooserBuilder<>(myRegionsList);
        myPopup = popupBuilder
            .setTitle(IdeLocalize.gotoCustomRegionCommand().get())
            .setResizable(false)
            .setMovable(false)
            .setItemChoosenCallback(() -> {
                PsiElement navigationElement = getNavigationElement();
                if (navigationElement != null) {
                    navigateTo(editor, navigationElement);
                    IdeDocumentHistory.getInstance(project).includeCurrentCommandAsNavigation();
                }
            })
            .createPopup();
    }

    void show() {
        myEditor.showPopupInBestPositionFor(myPopup);
    }

    private static class MyListModel extends DefaultListModel {
        private MyListModel(Collection<FoldingDescriptor> descriptors) {
            for (FoldingDescriptor descriptor : descriptors) {
                //noinspection unchecked
                super.addElement(new MyFoldingDescriptorWrapper(descriptor));
            }
        }
    }

    private static class MyFoldingDescriptorWrapper {
        private final 
        FoldingDescriptor myDescriptor;

        private MyFoldingDescriptorWrapper(FoldingDescriptor descriptor) {
            myDescriptor = descriptor;
        }

        
        public FoldingDescriptor getDescriptor() {
            return myDescriptor;
        }

        @Nullable
        @Override
        public String toString() {
            return myDescriptor.getPlaceholderText();
        }
    }

    public @Nullable PsiElement getNavigationElement() {
        Object selection = myRegionsList.getSelectedValue();
        if (selection instanceof MyFoldingDescriptorWrapper) {
            return ((MyFoldingDescriptorWrapper) selection).getDescriptor().getElement().getPsi();
        }
        return null;
    }

    private static Collection<FoldingDescriptor> orderByPosition(Collection<FoldingDescriptor> descriptors) {
        List<FoldingDescriptor> sorted = new ArrayList<FoldingDescriptor>(descriptors.size());
        sorted.addAll(descriptors);
        Collections.sort(sorted, new Comparator<FoldingDescriptor>() {
            @Override
            public int compare(FoldingDescriptor descriptor1, FoldingDescriptor descriptor2) {
                int pos1 = descriptor1.getElement().getTextRange().getStartOffset();
                int pos2 = descriptor2.getElement().getTextRange().getStartOffset();
                return pos1 - pos2;
            }
        });
        return sorted;
    }

    private static void navigateTo(Editor editor, PsiElement element) {
        int offset = element.getTextRange().getStartOffset();
        if (offset >= 0 && offset < editor.getDocument().getTextLength()) {
            editor.getCaretModel().removeSecondaryCarets();
            editor.getCaretModel().moveToOffset(offset);
            editor.getScrollingModel().scrollToCaret(ScrollType.CENTER);
            editor.getSelectionModel().removeSelection();
        }
    }
}
