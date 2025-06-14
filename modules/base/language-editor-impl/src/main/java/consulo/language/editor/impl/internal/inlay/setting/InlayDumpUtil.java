// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor.impl.internal.inlay.setting;

import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorCustomElementRenderer;
import consulo.codeEditor.Inlay;
import consulo.codeEditor.InlayModel;
import consulo.document.Document;
import consulo.util.lang.CharArrayUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Utility for dumping and removing inlay hints in text form.
 * Note that the dump format is part of public API for previews and tests.
 */
public final class InlayDumpUtil {
    private static final Pattern inlayPattern = Pattern.compile(
        "^\\h*/\\*<# (block) (.*?)#>\\*/\\h*(?:\\r\\n|\\r|\\n)|/\\*<#(.*?)#>\\*/",
        Pattern.MULTILINE | Pattern.DOTALL
    );

    private InlayDumpUtil() {
    }

    public static String removeInlays(String text) {
        return inlayPattern.matcher(text).replaceAll("");
    }

    public static String dumpInlays(String sourceText, Editor editor) {
        return dumpInlays(sourceText, editor, null,
            (renderer, inlay) -> renderer.toString(), 0, false);
    }

    public static String dumpInlays(
        String sourceText,
        Editor editor,
        Predicate<Inlay<?>> filter,
        BiFunction<EditorCustomElementRenderer, Inlay<?>, String> renderer,
        int offsetShift,
        boolean indentBlockInlays
    ) {
        Document document = editor.getDocument();
        InlayModel model = editor.getInlayModel();

        List<Inlay<?>> inlineElements = model.getInlineElementsInRange(0, document.getTextLength());
        if (filter != null) {
            inlineElements = inlineElements.stream().filter(filter).collect(Collectors.toList());
        }
        List<Inlay<?>> afterLineElements = model.getAfterLineEndElementsInRange(0, document.getTextLength());
        if (filter != null) {
            afterLineElements = afterLineElements.stream().filter(filter).collect(Collectors.toList());
        }
        List<Inlay<?>> blockElements = model.getBlockElementsInRange(0, document.getTextLength());
        if (filter != null) {
            blockElements = blockElements.stream().filter(filter).collect(Collectors.toList());
        }

        List<InlayData> inlayData = new ArrayList<>();
        for (Inlay<?> inlay : inlineElements) {
            inlayData.add(new InlayData(inlay.getOffset(), InlayDumpPlacement.Inline,
                renderer.apply(inlay.getRenderer(), inlay)));
        }
        for (Inlay<?> inlay : afterLineElements) {
            inlayData.add(new InlayData(inlay.getOffset(), InlayDumpPlacement.Inline,
                renderer.apply(inlay.getRenderer(), inlay)));
        }
        for (Inlay<?> inlay : blockElements) {
            int anchor = document.getLineStartOffset(document.getLineNumber(inlay.getOffset()));
            inlayData.add(new InlayData(anchor, InlayDumpPlacement.BlockAbove,
                renderer.apply(inlay.getRenderer(), inlay)));
        }
        inlayData.sort(Comparator.comparingInt(d -> d.anchorOffset));

        return dumpInlays(sourceText, inlayData, offsetShift, indentBlockInlays);
    }

    public static String dumpInlays(String sourceText, List<InlayData> inlayData) {
        return dumpInlays(sourceText, inlayData, 0, false);
    }

    public static String dumpInlays(
        String sourceText,
        List<InlayData> inlayData,
        int offsetShift,
        boolean indentBlockInlays
    ) {
        StringBuilder sb = new StringBuilder();
        int currentOffset = 0;
        for (InlayData data : inlayData) {
            int renderOffset = data.anchorOffset + offsetShift;
            sb.append(sourceText, currentOffset, renderOffset);
            if (data.placement == InlayDumpPlacement.BlockAbove && indentBlockInlays) {
                int indentStart = CharArrayUtil.shiftBackwardUntil(sourceText, renderOffset, "\n") + 1;
                int indentEnd = CharArrayUtil.shiftForward(sourceText, renderOffset, " \t");
                sb.append(sourceText, indentStart, indentEnd);
            }
            appendInlay(sb, data.content, data.placement);
            currentOffset = renderOffset;
        }
        sb.append(sourceText.substring(currentOffset));
        return sb.toString();
    }

    private static void appendInlay(StringBuilder sb, String content, InlayDumpPlacement placement) {
        sb.append("/*<# ");
        if (placement == InlayDumpPlacement.BlockAbove) {
            sb.append("block ");
        }
        sb.append(content).append(" #>*/");
        if (placement == InlayDumpPlacement.BlockAbove) {
            sb.append('\n');
        }
    }

    public static List<InlayData> extractInlays(String text) {
        Matcher matcher = inlayPattern.matcher(text);
        List<InlayData> extracted = new ArrayList<>();
        int prevNoInlays = 0;
        int prevWithInlays = 0;
        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();
            prevNoInlays += start - prevWithInlays;
            prevWithInlays = end;
            InlayDumpPlacement place = "block".equals(matcher.group(1))
                ? InlayDumpPlacement.BlockAbove : InlayDumpPlacement.Inline;
            int offset = prevNoInlays;
            String content = matcher.group(2) != null ? matcher.group(2) : matcher.group(3);
            extracted.add(new InlayData(offset, place, content));
        }
        return extracted;
    }

    public static class InlayData {
        public final int anchorOffset;
        public final InlayDumpPlacement placement;
        public final String content;

        public InlayData(int anchorOffset, InlayDumpPlacement placement, String content) {
            this.anchorOffset = anchorOffset;
            this.placement = placement;
            this.content = content;
        }
    }

    public enum InlayDumpPlacement {
        Inline,
        BlockAbove
    }
}
