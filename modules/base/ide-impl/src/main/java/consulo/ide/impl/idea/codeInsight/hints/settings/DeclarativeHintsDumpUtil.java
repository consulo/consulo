package consulo.ide.impl.idea.codeInsight.hints.settings;

import consulo.codeEditor.Editor;
import consulo.codeEditor.Inlay;
import consulo.ide.impl.idea.codeInsight.hints.DeclarativeIndentedBlockInlayRenderer;
import consulo.ide.impl.idea.codeInsight.hints.DeclarativeInlayRendererBase;
import consulo.ide.impl.idea.codeInsight.hints.InlayPresentationList;
import consulo.language.editor.impl.internal.inlay.setting.InlayDumpUtil;
import consulo.language.editor.inlay.*;
import consulo.util.lang.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public final class DeclarativeHintsDumpUtil {
    private DeclarativeHintsDumpUtil() {
    }

    public static List<ExtractedHintInfo> extractHints(String source) throws ParserException {
        List<InlayDumpUtil.InlayData> extractedInlays = InlayDumpUtil.extractInlays(source);
        List<ExtractedHintInfo> extractedHints = new ArrayList<>();
        DeclarativeInlayPosition position = new DeclarativeInlayPosition.InlineInlayPosition(0, true);
        HintFormat hintFormat = HintFormat.DEFAULT;

        int verticalPriorityCounter = 0;
        InlayContentParser inlayContentParser = new InlayContentParser();

        for (InlayDumpUtil.InlayData inlayData : extractedInlays) {
            int inlayOffset = inlayData.anchorOffset;
            InlayDumpUtil.InlayDumpPlacement renderType = inlayData.placement;
            String inlayContent = inlayData.content;

            if (renderType == InlayDumpUtil.InlayDumpPlacement.Inline) {
                position = new DeclarativeInlayPosition.InlineInlayPosition(inlayOffset, true);
            }
            else {
                position = new DeclarativeInlayPosition.AboveLineIndentedPosition(inlayOffset, verticalPriorityCounter++);
            }

            if (renderType == InlayDumpUtil.InlayDumpPlacement.Inline) {
                List<InlayPart> parts = inlayContentParser.parse("[" + inlayContent + "]");
                if (parts.isEmpty()) {
                    parseFail("Expected hint content");
                }
                else if (parts.size() == 1) {
                    InlayPart.HintContent contentPart = (InlayPart.HintContent) parts.get(0);
                    extractedHints.add(new ExtractedHintInfo(position, contentPart.text.trim(), hintFormat));
                }
                else {
                    if (inlayContent.isEmpty()) {
                        throw new ParserException("Inlay content is empty, but multiple parts found.");
                    }
                    if (inlayContent.charAt(0) != ']') {
                        parseFail("Expected ']', got '" + inlayContent.charAt(0) + "'. Perhaps there's unpaired unescaped ']' in your hint.");
                    }
                    if (inlayContent.charAt(inlayContent.length() - 1) != '[') {
                        parseFail("Expected '[', got '" + inlayContent.charAt(inlayContent.length() - 1) + "'. Perhaps there's unpaired unescaped ']' in your hint.");
                    }
                    String content = null;
                    int hintContentCounter = 0;
                    for (InlayPart part : parts) {
                        if (part instanceof InlayPart.Directive) {
                            hintFormat = parseFmtDirective((InlayPart.Directive) part, hintFormat);
                        }
                        else if (part instanceof InlayPart.HintContent) {
                            hintContentCounter++;
                            String text = ((InlayPart.HintContent) part).text;
                            if (!text.isEmpty()) {
                                if (content != null) {
                                    parseFail("Only single hint ('[...]') is allowed in inline inlay");
                                }
                                content = text;
                            }
                        }
                    }
                    if (hintContentCounter > 3) {
                        parseFail("Only one hint is allowed in inline inlay");
                    }
                    if (content != null) {
                        extractedHints.add(new ExtractedHintInfo(position, content, hintFormat));
                    }
                }
            }
            else {
                List<InlayPart> parts = inlayContentParser.parse(inlayContent);
                for (InlayPart part : parts) {
                    if (part instanceof InlayPart.Directive) {
                        hintFormat = parseFmtDirective((InlayPart.Directive) part, hintFormat);
                    }
                    else if (part instanceof InlayPart.HintContent) {
                        extractedHints.add(new ExtractedHintInfo(position, ((InlayPart.HintContent) part).text, hintFormat));
                    }
                }
            }
        }

        return extractedHints;
    }

    public static String dumpHints(String sourceText,
                                   Editor editor,
                                   Function<InlayPresentationList, String> renderer) {
        return InlayDumpUtil.dumpInlays(
            sourceText,
            editor,
            inlay -> inlay.getRenderer() instanceof DeclarativeInlayRendererBase<?>,
            (rendererInstance, inlay) -> {
                DeclarativeInlayRendererBase<?> declarativeRenderer = (DeclarativeInlayRendererBase<?>) rendererInstance;
                if (inlay.getPlacement() == Inlay.Placement.INLINE
                    || inlay.getPlacement() == Inlay.Placement.AFTER_LINE_END) {
                    List<InlayPresentationList> lists = declarativeRenderer.getPresentationLists();
                    if (lists.size() != 1) {
                        throw new RuntimeException("Inline declarative inlay must carry exactly one hint");
                    }
                    return renderer.apply(lists.get(0));
                }
                if (!(declarativeRenderer instanceof DeclarativeIndentedBlockInlayRenderer)) {
                    throw new AssertionError("Expected block renderer");
                }
                List<InlayPresentationList> lists = declarativeRenderer.getPresentationLists();
                if (lists.isEmpty()) {
                    throw new RuntimeException("Declarative inlay renderer must carry at least one hint");
                }
                StringBuilder sb = new StringBuilder();
                sb.append('[').append(renderer.apply(lists.get(0))).append(']');
                for (int i = 1; i < lists.size(); i++) {
                    sb.append(' ').append('[').append(renderer.apply(lists.get(i))).append(']');
                }
                return sb.toString();
            },
            0,
            true
        );
    }

    private static void parseFail(String msg) {
        throw new ParserException(msg);
    }

    private static HintFormat parseFmtDirective(InlayPart.Directive directive,
                                                HintFormat currentFormat) {
        if (!"fmt".equals(directive.id)) {
            parseFail("Unknown directive '" + directive.id + "'");
        }
        HintFormat newFormat = currentFormat;
        for (Pair<String, String> opt : directive.options) {
            String key = opt.getFirst().toLowerCase();
            String value = opt.getSecond();
            switch (key) {
                case "colorkind":
                    newFormat = newFormat.withColorKind(Enum.valueOf(HintColorKind.class, value));
                    break;
                case "fontsize":
                    newFormat = newFormat.withFontSize(Enum.valueOf(HintFontSize.class, value));
                    break;
                case "marginpadding":
                    newFormat = newFormat.withHorizontalMargin(Enum.valueOf(HintMarginPadding.class, value));
                    break;
                default:
                    parseFail("Unknown declarative hint 'fmt' directive option '" + key + "'");
            }
        }
        return newFormat;
    }

    public static class ExtractedHintInfo {
        public final DeclarativeInlayPosition position;
        public final String text;
        public final HintFormat hintFormat;

        public ExtractedHintInfo(DeclarativeInlayPosition position, String text, HintFormat hintFormat) {
            this.position = position;
            this.text = text;
            this.hintFormat = hintFormat;
        }
    }

    public static class ParserException extends RuntimeException {
        public ParserException(String message) {
            super(message);
        }
    }

    private interface InlayPart {
        class HintContent implements InlayPart {
            final String text;

            HintContent(String text) {
                this.text = text;
            }
        }

        class Directive implements InlayPart {
            final String id;
            final List<Pair<String, String>> options;

            Directive(String id, List<Pair<String, String>> options) {
                this.id = id;
                this.options = options;
            }
        }
    }

    private static class InlayContentParser {
        private String text;
        private int offset;
        private final List<InlayPart> inlayParts = new ArrayList<>();
        private String currentDirId;
        private String currentOptKey;
        private String currentOptValue;
        private final StringBuilder contentBuilder = new StringBuilder();

        public List<InlayPart> parse(String text) {
            this.text = text;
            this.offset = 0;
            inlayParts.clear();
            parseMeta();
            return new ArrayList<>(inlayParts);
        }

        private void parseMeta() {
            while (hasNextChar()) {
                char ch = nextChar();
                if (ch == '[') {
                    parseContent();
                }
                else if (Character.isJavaIdentifierStart(ch)) {
                    offset--;
                    parseDir();
                }
                else if (Character.isWhitespace(ch)) {
                    // skip
                }
                else {
                    fail("Unexpected meta character '" + ch + "'");
                }
            }
        }

        private void parseContent() {
            contentBuilder.setLength(0);
            int nestingLevel = 1;
            while (hasNextChar()) {
                char ch = nextChar();
                if (ch == '[') {
                    nestingLevel++;
                    contentBuilder.append(ch);
                }
                else if (ch == ']') {
                    nestingLevel--;
                    if (nestingLevel > 0) {
                        contentBuilder.append(ch);
                    }
                    else {
                        inlayParts.add(new InlayPart.HintContent(contentBuilder.toString()));
                        return;
                    }
                }
                else if (ch == '\\') {
                    parseContentEscape();
                }
                else {
                    contentBuilder.append(ch);
                }
            }
            fail("Expected ']'\n  in '" + text + "'");
        }

        private void parseContentEscape() {
            if (!hasNextChar()) fail("Expected character after '\\\\'");
            char ch = nextChar();
            if (ch == '\\' || ch == '[' || ch == ']') {
                contentBuilder.append(ch);
            }
            else {
                fail("Unknown escape sequence '\\" + ch + "'");
            }
        }

        private void parseDir() {
            parseDirId();
            List<Pair<String, String>> opts = new ArrayList<>();
            boolean more;
            do {
                more = parseDirOpt();
                opts.add(new Pair<>(currentOptKey, currentOptValue));
            }
            while (more);
            inlayParts.add(new InlayPart.Directive(currentDirId, opts));
        }

        private void parseDirId() {
            int start = offset;
            while (hasNextChar()) {
                char ch = nextChar();
                if (ch == ':') {
                    currentDirId = text.substring(start, offset - 1);
                    return;
                }
                else if (!Character.isJavaIdentifierPart(ch)) {
                    fail("Unexpected character '" + ch + "' in directive id");
                }
            }
            fail("Expected ':' after directive id");
        }

        private boolean parseDirOpt() {
            parseDirOptKey();
            return parseDirOptValue();
        }

        private void parseDirOptKey() {
            skipWhitespace();
            int start = offset;
            if (hasNextChar()) {
                char ch = nextChar();
                if (!Character.isJavaIdentifierStart(ch)) {
                    fail("Expected valid java identifier start char, got '" + ch + "'");
                }
            }
            while (hasNextChar()) {
                char ch = nextChar();
                if (ch == '=') {
                    currentOptKey = text.substring(start, offset - 1);
                    return;
                }
                else if (!Character.isJavaIdentifierPart(ch)) {
                    fail("Unexpected character '" + ch + "' in directive key");
                }
            }
            fail("Expected '=' after directive key");
        }

        private boolean parseDirOptValue() {
            int start = offset;
            while (hasNextChar()) {
                char ch = nextChar();
                if (ch == ',') {
                    currentOptValue = text.substring(start, offset - 1);
                    return true;
                }
                else if (Character.isWhitespace(ch)) {
                    currentOptValue = text.substring(start, offset - 1);
                    return false;
                }
            }
            currentOptValue = text.substring(start, offset);
            return false;
        }

        private void fail(String msg) {
            parseFail(msg + "\n  at " + offset + "\n  in '" + text + "'");
        }

        private char nextChar() {
            return text.charAt(offset++);
        }

        private boolean hasNextChar() {
            return offset < text.length();
        }

        private void skipWhitespace() {
            while (hasNextChar() && Character.isWhitespace(text.charAt(offset))) {
                offset++;
            }
        }
    }
}
