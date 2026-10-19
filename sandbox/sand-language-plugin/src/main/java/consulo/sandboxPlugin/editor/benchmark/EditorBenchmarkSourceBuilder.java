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
package consulo.sandboxPlugin.editor.benchmark;

import consulo.codeEditor.CodeInsightColors;
import consulo.codeEditor.DefaultLanguageHighlighterColors;
import consulo.codeEditor.EditorColors;
import consulo.codeEditor.markup.HighlighterLayer;
import consulo.colorScheme.TextAttributesKey;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Random;

/**
 * @author VISTALL
 * @since 2026-10-19
 */
public class EditorBenchmarkSourceBuilder {
    private static final String[] NOUNS = {
        "name", "value", "index", "count", "result", "buffer", "entry", "node", "element", "item", "key", "offset", "length",
        "handler", "listener", "context", "project", "module", "file", "document", "editor", "range", "marker", "token", "state",
        "cache", "config", "option", "provider", "service", "request", "response", "session", "channel", "record", "target"
    };

    private static final String[] WORDS = {
        "alpha", "beta", "gamma", "delta", "lorem", "ipsum", "dolor", "sit", "amet", "consectetur", "adipiscing", "elit", "sed",
        "eiusmod", "tempor", "incididunt", "labore", "magna", "aliqua", "veniam", "nostrud", "exercitation", "ullamco", "laboris"
    };

    private static final String[] IMPORT_PACKAGES = {
        "java.util", "java.util.concurrent", "java.util.function", "java.io", "java.nio.file", "java.time", "java.net", "java.text",
        "java.util.stream", "java.lang.ref", "java.util.regex", "java.util.concurrent.atomic"
    };

    private static final String[] IMPORT_CLASSES = {
        "ArrayList", "HashMap", "Optional", "Function", "Supplier", "Path", "Files", "Duration", "Instant", "URI", "Pattern",
        "Collectors", "AtomicInteger", "WeakReference", "LinkedHashMap", "TreeSet", "Executor", "CompletableFuture", "Predicate",
        "BufferedReader", "StandardCharsets", "MessageFormat", "Matcher", "AtomicReference"
    };

    private final Random myRandom;
    private final StringBuilder myText = new StringBuilder();
    private final List<EditorBenchmarkSpan> mySpans = new ArrayList<>();
    private final List<EditorBenchmarkFold> myFolds = new ArrayList<>();
    private final List<EditorBenchmarkHint> myHints = new ArrayList<>();
    private final List<EditorBenchmarkMethod> myMethods = new ArrayList<>();
    private final List<String> myListFields = new ArrayList<>();
    private final List<String> myMapFields = new ArrayList<>();
    private int myLine;
    private int myLongLine = -1;
    private int myVeryLongLine = -1;

    public EditorBenchmarkSourceBuilder(long seed) {
        myRandom = new Random(seed);
    }

    public EditorBenchmarkSource build(int methodCount) {
        header();
        imports();
        classStart();
        fields();
        tables();
        for (int i = 0; i < methodCount; i++) {
            method(i);
        }
        line("}");

        String text = myText.toString();
        return new EditorBenchmarkSource(
            text,
            List.copyOf(mySpans),
            List.copyOf(myFolds),
            List.copyOf(myHints),
            List.copyOf(myMethods),
            typicalLine(text),
            myLongLine,
            myVeryLongLine
        );
    }

    private void header() {
        int start = myText.length();
        line("/*");
        for (int i = 0; i < 12; i++) {
            line(" * " + sentence(8));
        }
        append(" */");
        myFolds.add(new EditorBenchmarkFold(start, myText.length(), "/.../", true));
        newLine();
        newLine();
        line("package consulo.benchmark.generated;");
        newLine();
    }

    private void imports() {
        int start = myText.length() + "import ".length();
        int end = start;
        for (int i = 0; i < 240; i++) {
            String pkg = IMPORT_PACKAGES[i % IMPORT_PACKAGES.length];
            String cls = IMPORT_CLASSES[(i * 7) % IMPORT_CLASSES.length] + (i < IMPORT_CLASSES.length ? "" : String.valueOf(i));
            append("import " + pkg + ".«C:" + cls + "»;");
            end = myText.length();
            newLine();
        }
        myFolds.add(new EditorBenchmarkFold(start, end, "...", true));
        newLine();
    }

    private void classStart() {
        line("/**");
        line(" * " + sentence(10));
        line(" * " + sentence(9));
        line(" *");
        line(" * «G:@author» generator");
        line(" */");
        line("@SuppressWarnings(\"unchecked\")");
        line("public class «K:EditorBenchmarkSample»<«C:T» extends «C:Comparable»<«C:T»>> extends «C:AbstractSample» "
            + "implements «C:Runnable», «C:Serializable» {");
        line("    private static final «C:Logger» «S:LOG» = «C:Logger».«T:getInstance»(«C:EditorBenchmarkSample».class);");
        newLine();
    }

    private void fields() {
        for (int i = 0; i < 300; i++) {
            String noun = noun();
            switch (i % 6) {
                case 0 -> line("    private static final int «S:LIMIT_" + i + "» = " + number() + ";");
                case 1, 2 -> {
                    String field = "my" + capitalize(noun) + i;
                    myListFields.add(field);
                    line("    private final «C:List»<«C:String»> «F:" + field + "» = new «C:ArrayList»<>();");
                }
                case 3 -> {
                    String field = "my" + capitalize(noun) + "Index" + i;
                    myMapFields.add(field);
                    line("    private final «C:Map»<«C:String», «C:Integer»> «F:" + field + "» = new «C:HashMap»<>(" + number() + ");");
                }
                case 4 -> {
                    line("    @Deprecated");
                    line("    private «C:String» «X:«F:my" + capitalize(noun) + "Legacy" + i + "»» = \"" + word() + "\";");
                }
                default -> line("    private «C:Duration» «N:«F:my" + capitalize(noun) + "Timeout" + i + "»» = «C:Duration».«T:ofSeconds»("
                    + number() + ");");
            }
        }
        newLine();
    }

    private void tables() {
        StringBuilder banner = new StringBuilder("    private static final «C:String» «S:BANNER» = \"");
        while (banner.length() < 500) {
            banner.append(sentence(6)).append(' ');
        }
        banner.append("\";");
        myLongLine = myLine;
        line(banner.toString());

        StringBuilder table = new StringBuilder("    private static final int[] «S:TABLE» = {");
        int value = 0;
        while (table.length() < 2400) {
            table.append(value).append(", ");
            value += 1 + myRandom.nextInt(97);
        }
        table.append(value).append("};");
        myVeryLongLine = myLine;
        line(table.toString());
        newLine();
    }

    private void method(int index) {
        String p0 = noun();
        String p1 = p0 + "Count";
        String p2 = p0 + "Index";
        String name = "compute" + capitalize(noun()) + index;

        int separatorOffset = myText.length();
        int docStart = myText.length() + 4;
        line("    /**");
        line("     * Computes the " + noun() + " for «H:{@code " + p0 + "}» " + sentence(5) + ".");
        line("     *");
        line("     * «G:@param» " + p0 + " the " + word());
        line("     * «G:@param» " + p1 + " the " + word());
        line("     * «G:@param» " + p2 + " the " + word());
        line("     * «G:@return» the computed value");
        append("     */");
        myFolds.add(new EditorBenchmarkFold(docStart, myText.length(), "/** Computes the ... */", false));
        newLine();

        boolean overriding = index % 3 == 0;
        if (overriding) {
            line("    @Override");
        }

        append("    public «C:List»<«C:String»> ");
        int nameStart = myText.length();
        append("«D:" + name + "»");
        int nameEnd = myText.length();
        append("(«C:String» «P:" + p0 + "», int «P:" + p1 + "», «C:Map»<«C:String», «C:Integer»> «P:" + p2 + "») {");
        int bodyStart = myText.length() - 1;
        newLine();

        myMethods.add(new EditorBenchmarkMethod(separatorOffset, nameStart, nameEnd, overriding, index % 4 == 1));

        String listField = pick(myListFields);
        String mapField = pick(myMapFields);
        String usageField = myListFields.get(3);

        int statements = 10 + myRandom.nextInt(13);
        for (int s = 0; s < statements; s++) {
            String local = noun() + "Value" + s;
            switch (myRandom.nextInt(9)) {
                case 0, 1 -> line("        «C:String» «L:" + local + "» = «P:" + p0 + "» + \"_\" + «P:" + p1 + "» + \" = \" + «P:" + p2
                    + "».«M:getOrDefault»(«P:" + p0 + "», " + number() + ");");
                case 2 -> {
                    line("        for (int «L:i» = 0; «L:i» < «P:" + p1 + "»; «L:i»++) {");
                    line("            «C:String» «L:" + local + "» = «P:" + p0 + "».«M:substring»(0, «C:Math».«T:min»(«L:i», «P:" + p0
                        + "».«M:length»())) + «L:i»;");
                    line("            if («L:" + local + "».«M:length»() > " + number() + " && !«L:" + local + "».«M:startsWith»(\"" + word()
                        + "\")) {");
                    line("                «F:" + listField + "».«M:add»(«L:" + local + "».«M:toUpperCase»(«C:Locale».«S:ROOT»));");
                    line("            }");
                    line("            else {");
                    line("                «S:LOG».«M:warn»(\"skipped " + word() + " \" + «L:" + local + "»);");
                    line("            }");
                    line("        }");
                }
                case 3 -> line("        «F:" + listField + "».«M:stream»().«M:filter»(it -> it.«M:startsWith»(\"" + word()
                    + "\")).«M:map»(«C:String»::«M:trim»).«M:forEach»(«F:" + mapField + "»::«M:remove»);");
                case 4 -> line("        «M:process»(«P:" + p0 + "», ‹limit›" + number() + ", ‹enabled›true, ‹fallback›null);");
                case 5 -> {
                    line("        try {");
                    line("            «C:String» «L:" + local + "» = «C:Files».«T:readString»(«C:Path».«T:of»(\"" + word() + "\", «P:" + p0
                        + "» + \".txt\"));");
                    line("            «F:" + mapField + "».«M:put»(«L:" + local + "», «L:" + local + "».«M:length»());");
                    line("        }");
                    line("        catch («C:IOException» «P:e») {");
                    line("            throw new «C:IllegalStateException»(\"cannot read \" + «P:" + p0 + "», «P:e»);");
                    line("        }");
                }
                case 6 -> {
                    if (myRandom.nextInt(12) == 0) {
                        line("        // «O:TODO " + sentence(7) + "»");
                    }
                    else {
                        line("        // " + sentence(9));
                    }
                }
                case 7 -> line("        «F:«U:" + usageField + "»».«M:add»(«P:" + p0 + "»);");
                default -> line("        «F:" + mapField + "».«M:merge»(«P:" + p0 + "», " + number() + ", «C:Integer»::«T:sum»);");
            }
        }

        if (index % 40 == 7) {
            line("        «E:«R:undefinedCall" + index + "»(«P:" + p0 + "»)»;");
        }
        if (index % 6 == 2) {
            line("        «C:String» «N:unused" + index + "» = «W:(«C:String») «P:" + p0 + "»»;");
        }
        if (index % 9 == 4) {
            line("        «X:«M:oldApi»»(«P:" + p0 + "»);");
        }
        if (index % 5 == 3) {
            line("        «C:String» «L:joined" + index + "» = «Y:\"" + word() + "\" + \"" + word() + "\"»;");
        }
        if (index % 50 == 25) {
            StringBuilder longLine = new StringBuilder("        «C:String» «L:message" + index + "» = \"" + sentence(4) + "\"");
            while (longLine.length() < 320) {
                longLine.append(" + «P:").append(p0).append("» + \"").append(sentence(3)).append('"');
            }
            line(longLine + ";");
        }

        append("        return «F:" + listField + "».«M:isEmpty»() ? «C:Collections».«T:emptyList»() : «C:Collections».«T:unmodifiableList»(«F:"
            + listField + "»);");
        newLine();
        append("    }");
        myFolds.add(new EditorBenchmarkFold(bodyStart, myText.length(), "{...}", index % 5 == 4));
        newLine();
        newLine();
    }

    private int typicalLine(String text) {
        int middle = myLine / 2;
        int line = 0;
        int lineStart = 0;
        int best = -1;
        for (int i = 0; i <= text.length(); i++) {
            if (i == text.length() || text.charAt(i) == '\n') {
                int length = i - lineStart;
                if (line >= middle && length >= 100 && length <= 140) {
                    best = line;
                    break;
                }
                line++;
                lineStart = i + 1;
            }
        }
        return best < 0 ? middle : best;
    }

    private void line(String template) {
        append(template);
        newLine();
    }

    private void newLine() {
        myText.append('\n');
        myLine++;
    }

    private void append(String template) {
        Deque<int[]> open = new ArrayDeque<>();
        int i = 0;
        while (i < template.length()) {
            char c = template.charAt(i);
            if (c == '«') {
                open.push(new int[]{myText.length(), template.charAt(i + 1)});
                i += 3;
            }
            else if (c == '»') {
                int[] span = open.pop();
                addSpan(span[0], myText.length(), (char) span[1]);
                i++;
            }
            else if (c == '‹') {
                int close = template.indexOf('›', i);
                myHints.add(new EditorBenchmarkHint(myText.length(), template.substring(i + 1, close) + ":"));
                i = close + 1;
            }
            else {
                myText.append(c);
                i++;
            }
        }
    }

    private void addSpan(int start, int end, char kind) {
        TextAttributesKey key;
        int layer = HighlighterLayer.ADDITIONAL_SYNTAX;
        switch (kind) {
            case 'L' -> key = DefaultLanguageHighlighterColors.LOCAL_VARIABLE;
            case 'P' -> key = DefaultLanguageHighlighterColors.PARAMETER;
            case 'F' -> key = DefaultLanguageHighlighterColors.INSTANCE_FIELD;
            case 'S' -> key = DefaultLanguageHighlighterColors.STATIC_FIELD;
            case 'M' -> key = DefaultLanguageHighlighterColors.INSTANCE_METHOD;
            case 'T' -> key = DefaultLanguageHighlighterColors.STATIC_METHOD;
            case 'C' -> key = DefaultLanguageHighlighterColors.CLASS_REFERENCE;
            case 'D' -> key = DefaultLanguageHighlighterColors.FUNCTION_DECLARATION;
            case 'K' -> key = DefaultLanguageHighlighterColors.CLASS_NAME;
            case 'G' -> key = DefaultLanguageHighlighterColors.DOC_COMMENT_TAG;
            case 'H' -> key = DefaultLanguageHighlighterColors.DOC_COMMENT_MARKUP;
            case 'O' -> key = CodeInsightColors.TODO_DEFAULT_ATTRIBUTES;
            case 'N' -> {
                key = CodeInsightColors.NOT_USED_ELEMENT_ATTRIBUTES;
                layer = HighlighterLayer.WARNING;
            }
            case 'W' -> {
                key = CodeInsightColors.WARNINGS_ATTRIBUTES;
                layer = HighlighterLayer.WARNING;
            }
            case 'X' -> {
                key = CodeInsightColors.DEPRECATED_ATTRIBUTES;
                layer = HighlighterLayer.WARNING;
            }
            case 'Y' -> {
                key = CodeInsightColors.WEAK_WARNING_ATTRIBUTES;
                layer = HighlighterLayer.WEAK_WARNING;
            }
            case 'E' -> {
                key = CodeInsightColors.ERRORS_ATTRIBUTES;
                layer = HighlighterLayer.ERROR;
            }
            case 'R' -> {
                key = CodeInsightColors.WRONG_REFERENCES_ATTRIBUTES;
                layer = HighlighterLayer.ERROR;
            }
            case 'U' -> {
                key = EditorColors.IDENTIFIER_UNDER_CARET_ATTRIBUTES;
                layer = HighlighterLayer.ELEMENT_UNDER_CARET;
            }
            default -> throw new IllegalArgumentException("Unknown span kind " + kind);
        }
        mySpans.add(new EditorBenchmarkSpan(start, end, key, layer));
    }

    private String noun() {
        return NOUNS[myRandom.nextInt(NOUNS.length)];
    }

    private String word() {
        return WORDS[myRandom.nextInt(WORDS.length)];
    }

    private String sentence(int words) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < words; i++) {
            if (i > 0) {
                builder.append(' ');
            }
            builder.append(word());
        }
        return builder.toString();
    }

    private String number() {
        return String.valueOf(myRandom.nextInt(1000));
    }

    private String pick(List<String> values) {
        return values.get(myRandom.nextInt(values.size()));
    }

    private static String capitalize(String value) {
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }
}
