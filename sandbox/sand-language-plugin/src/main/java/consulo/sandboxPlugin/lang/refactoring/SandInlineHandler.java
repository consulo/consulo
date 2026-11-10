/*
 * Copyright 2013-2026 consulo.io
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
package consulo.sandboxPlugin.lang.refactoring;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.access.RequiredWriteAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Editor;
import consulo.language.Language;
import consulo.language.editor.refactoring.inline.InlineHandler;
import consulo.language.impl.ast.LeafElement;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReference;
import consulo.language.psi.PsiWhiteSpace;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.localize.LocalizeValue;
import consulo.sandboxPlugin.lang.SandLanguage;
import consulo.sandboxPlugin.lang.psi.SandClass;
import consulo.sandboxPlugin.lang.psi.SandExtendsRef;
import consulo.sandboxPlugin.lang.psi.SandTokens;
import consulo.usage.UsageInfo;
import consulo.util.collection.MultiMap;
import org.jspecify.annotations.Nullable;

/**
 * Inlines a class into its subclasses: each one extends the inlined class's parent instead, and the class is removed.
 *
 * @author VISTALL
 * @since 2026-11-10
 */
@ExtensionImpl
public class SandInlineHandler implements InlineHandler {
    private static final Settings INLINE_ALL = () -> false;

    @Override
    public @Nullable Settings prepareInlineElement(PsiElement element, @Nullable Editor editor, boolean invokedOnReference) {
        return element instanceof SandClass ? INLINE_ALL : null;
    }

    @Override
    @RequiredWriteAction
    public void removeDefinition(PsiElement element, Settings settings) {
        element.delete();
    }

    @Override
    public @Nullable Inliner createInliner(PsiElement element, Settings settings) {
        return element instanceof SandClass ? new ClassInliner() : null;
    }

    @Override
    public Language getLanguage() {
        return SandLanguage.INSTANCE;
    }

    private static class ClassInliner implements Inliner {
        @Override
        @RequiredReadAction
        public @Nullable MultiMap<PsiElement, LocalizeValue> getConflicts(PsiReference reference, PsiElement referenced) {
            String parentName = parentName(referenced);
            SandClass subclass = PsiTreeUtil.getParentOfType(reference.getElement(), SandClass.class);
            if (parentName == null || subclass == null || !parentName.equals(subclass.getName())) {
                return null;
            }
            MultiMap<PsiElement, LocalizeValue> conflicts = new MultiMap<>();
            conflicts.putValue(subclass, LocalizeValue.localizeTODO("Class " + parentName + " would extend itself"));
            return conflicts;
        }

        @Override
        @RequiredWriteAction
        public void inlineUsage(UsageInfo usage, PsiElement referenced) {
            if (!(usage.getElement() instanceof SandExtendsRef extendsRef)) {
                return;
            }
            String parentName = parentName(referenced);
            if (parentName != null) {
                if (extendsRef.getFirstChild().getNode() instanceof LeafElement leaf) {
                    leaf.replaceWithText(parentName);
                }
                return;
            }
            PsiElement colon = PsiTreeUtil.skipWhitespacesBackward(extendsRef);
            if (colon == null || colon.getNode().getElementType() != SandTokens.COLON) {
                return;
            }
            PsiElement start = colon.getPrevSibling() instanceof PsiWhiteSpace whiteSpace ? whiteSpace : colon;
            extendsRef.getParent().deleteChildRange(start, extendsRef);
        }

        @RequiredReadAction
        private static @Nullable String parentName(PsiElement referenced) {
            SandExtendsRef parent = PsiTreeUtil.getChildOfType(referenced, SandExtendsRef.class);
            return parent != null ? parent.getText() : null;
        }
    }
}
