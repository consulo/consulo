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
package consulo.sandboxPlugin.lang.documentation;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.documentation.DocumentationManagerProtocol;
import consulo.language.editor.documentation.DocumentationMarkup;
import consulo.language.editor.documentation.LanguageDocumentationProvider;
import consulo.language.psi.PsiComment;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiManager;
import consulo.language.psi.PsiWhiteSpace;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.sandboxPlugin.lang.SandLanguage;
import consulo.sandboxPlugin.lang.psi.SandClass;
import consulo.sandboxPlugin.lang.psi.SandExtendsRef;
import consulo.sandboxPlugin.lang.psi.stub.SandClassSearch;
import consulo.util.lang.xml.XmlStringUtil;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author VISTALL
 * @since 2026-10-09
 */
@ExtensionImpl
public class SandDocumentationProvider implements LanguageDocumentationProvider {
    @Override
    public Language getLanguage() {
        return SandLanguage.INSTANCE;
    }

    @RequiredReadAction
    @Override
    public @Nullable String generateDoc(PsiElement element, @Nullable PsiElement originalElement) {
        if (!(element instanceof SandClass sandClass)) {
            return null;
        }

        String name = XmlStringUtil.escapeText(String.valueOf(sandClass.getName()));

        StringBuilder builder = new StringBuilder();
        builder.append(DocumentationMarkup.DEFINITION_START).append("class <b>").append(name).append("</b>");

        SandExtendsRef extendsRef = PsiTreeUtil.getChildOfType(sandClass, SandExtendsRef.class);
        if (extendsRef != null) {
            String parent = XmlStringUtil.escapeText(extendsRef.getText());
            builder.append(" : <a href='").append(DocumentationManagerProtocol.PSI_ELEMENT_PROTOCOL).append(parent).append("'>")
                .append(parent).append("</a>");
        }
        builder.append(DocumentationMarkup.DEFINITION_END);

        builder.append(DocumentationMarkup.CONTENT_START);
        List<String> comment = collectComment(sandClass);
        if (comment.isEmpty()) {
            builder.append("<p>No description.</p>");
        }
        else {
            for (String line : comment) {
                builder.append(XmlStringUtil.escapeText(line)).append("<br>");
            }
        }
        builder.append(DocumentationMarkup.CONTENT_END);

        builder.append(DocumentationMarkup.SECTIONS_START);
        appendSection(builder, "File:", "<code>" + XmlStringUtil.escapeText(sandClass.getContainingFile().getName()) + "</code>");
        appendSection(builder, "Offset:", String.valueOf(sandClass.getTextOffset()));
        builder.append(DocumentationMarkup.SECTIONS_END);
        return builder.toString();
    }

    @RequiredReadAction
    @Override
    public @Nullable List<String> getUrlFor(PsiElement element, PsiElement originalElement) {
        if (element instanceof SandClass sandClass && sandClass.getName() != null) {
            return Collections.singletonList("https://consulo.io/sand/" + sandClass.getName());
        }
        return null;
    }

    @RequiredReadAction
    @Override
    public @Nullable PsiElement getDocumentationElementForLink(PsiManager psiManager, String link, PsiElement context) {
        Collection<SandClass> classes = SandClassSearch.active(psiManager.getProject(), link);
        return classes.isEmpty() ? null : classes.iterator().next();
    }

    private static void appendSection(StringBuilder builder, String title, String value) {
        builder.append(DocumentationMarkup.SECTION_HEADER_START).append(title).append(DocumentationMarkup.SECTION_SEPARATOR)
            .append("<p>").append(value).append(DocumentationMarkup.SECTION_END).append("</tr>");
    }

    @RequiredReadAction
    private static List<String> collectComment(SandClass sandClass) {
        List<String> lines = new ArrayList<>();
        PsiElement sibling = sandClass.getPrevSibling();
        while (sibling instanceof PsiWhiteSpace || sibling instanceof PsiComment) {
            if (sibling instanceof PsiComment comment) {
                lines.add(0, comment.getText().replaceFirst("^//", "").trim());
            }
            else if (sibling.getText().chars().filter(c -> c == '\n').count() > 1) {
                break;
            }
            sibling = sibling.getPrevSibling();
        }
        return lines;
    }
}
