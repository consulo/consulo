// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.url.reference;

import consulo.endpoint.presentation.HttpMethodPresentation;
import consulo.endpoint.url.Authority;
import consulo.endpoint.url.UrlPath;
import consulo.endpoint.url.UrlResolveRequest;
import consulo.endpoint.url.UrlTargetInfo;
import consulo.language.editor.completion.lookup.InsertHandler;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.LookupElementBuilder;
import consulo.language.editor.completion.lookup.PrioritizedLookupElement;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.StringJoiner;

public interface PathSegmentHandler {
    String render(UrlPath.PathSegment segment);

    List<UrlPath.PathSegment> getExactPrefix(UrlPath path);

    default InsertHandler<LookupElement> createInsertHandler(UrlTargetInfo variant, boolean hasSomethingNext) {
        return (context, item) -> {
        };
    }

    default @Nullable LookupElement createLookupElement(
        UrlResolveRequest context,
        List<? extends UrlPath.PathSegment> exactPrefix,
        List<? extends UrlPath.PathSegment> pathToComplete,
        UrlTargetInfo variant,
        String prefix,
        String suffix,
        boolean hasSomethingNext
    ) {
        String lookUp = joinRendered(exactPrefix);
        if (lookUp.isEmpty()) {
            return null;
        }
        String resultLookup = prefix + lookUp;
        String followingPath = removePrefix(removePrefix(joinRendered(pathToComplete), lookUp), suffix);
        LookupElement lookup =
            LookupElementBuilder.create(new UrlPathReference.UrlPathLookupObject(resultLookup), resultLookup + suffix)
                .withIcon(variant.getIcon())
                .withTailText(followingPath + " " + HttpMethodPresentation.getHttpMethodsPresentation(variant.getMethods()))
                .withTypeText(variant.getSource())
                .withPsiElement(variant.getDocumentationPsiElement())
                .withStrikeoutness(variant.isDeprecated())
                .withInsertHandler(createInsertHandler(variant, hasSomethingNext));

        @Nullable String authorityHint = context.getAuthorityHint();
        if (authorityHint != null) {
            for (Authority authority : variant.getAuthorities()) {
                if (authority instanceof Authority.Exact exact && exact.getText().equals(authorityHint)) {
                    lookup = PrioritizedLookupElement.withPriority(lookup, 20.0);
                    break;
                }
            }
        }

        return lookup;
    }

    private String joinRendered(List<? extends UrlPath.PathSegment> segments) {
        StringJoiner joiner = new StringJoiner("/");
        for (UrlPath.PathSegment segment : segments) {
            joiner.add(render(segment));
        }
        return joiner.toString();
    }

    private static String removePrefix(String text, String prefix) {
        return text.startsWith(prefix) ? text.substring(prefix.length()) : text;
    }
}
