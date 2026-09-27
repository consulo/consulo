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
package consulo.web.ui.impl.internal.image;

import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.image.ImageKey;
import org.jspecify.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Description of a composed image, small enough to travel inside an url query and rebuilt by
 * {@link consulo.web.internal.servlet.UIIconServlet} on the other side.
 * <p/>
 * Group and image ids are lowercased paths of {@code [a-z0-9._]}, so {@code (}, {@code )} and {@code ,}
 * can delimit the text form without any escaping.
 *
 * @author VISTALL
 * @since 2026-08-01
 */
public sealed interface WebImageSpec {
    int DEFAULT_SIZE = 16;

    record Key(String groupId, String imageId, int width, int height) implements WebImageSpec {
    }

    record Empty(int width, int height) implements WebImageSpec {
    }

    record Colorize(WebImageSpec child, int rgb) implements WebImageSpec {
    }

    record Alpha(WebImageSpec child, float alpha) implements WebImageSpec {
    }

    record Resize(WebImageSpec child, int width, int height) implements WebImageSpec {
    }

    record Layered(List<WebImageSpec> children) implements WebImageSpec {
    }

    record Gray(WebImageSpec child, int percent) implements WebImageSpec {
    }

    record Append(WebImageSpec left, WebImageSpec right) implements WebImageSpec {
    }

    record Text(WebImageSpec child, String text) implements WebImageSpec {
    }

    record Busy(int width, int height) implements WebImageSpec {
        public WebImageSpec still() {
            if (width <= 0 || height <= 0) {
                return new Empty(Math.max(width, 0), Math.max(height, 0));
            }

            ImageKey key = PlatformIconGroup.processStep_passive();
            return new Key(key.getGroupId(), key.getImageId(), width, height);
        }
    }

    record Blinking(WebImageSpec child) implements WebImageSpec {
    }

    static WebImageSpec resize(WebImageSpec child, int width, int height) {
        return switch (child) {
            case Busy ignored -> new Busy(width, height);
            case Blinking inner -> blinking(resize(inner.child(), width, height));
            default -> new Resize(child, width, height);
        };
    }

    static WebImageSpec blinking(WebImageSpec child) {
        return switch (child) {
            case Busy busy -> busy;
            case Blinking blinking -> blinking;
            default -> new Blinking(child);
        };
    }

    static boolean animated(WebImageSpec spec) {
        return switch (spec) {
            case Key ignored -> false;
            case Empty ignored -> false;
            case Colorize colorize -> animated(colorize.child());
            case Alpha alpha -> animated(alpha.child());
            case Resize resize -> animated(resize.child());
            case Layered layered -> layered.children().stream().anyMatch(WebImageSpec::animated);
            case Gray gray -> animated(gray.child());
            case Append append -> animated(append.left()) || animated(append.right());
            case Text text -> animated(text.child());
            case Busy ignored -> true;
            case Blinking ignored -> true;
        };
    }

    static WebImageSpec still(WebImageSpec spec) {
        return freeze(spec, true);
    }

    static WebImageSpec withoutBlinking(WebImageSpec spec) {
        return freeze(spec, false);
    }

    private static WebImageSpec freeze(WebImageSpec spec, boolean busyToo) {
        if (!animated(spec)) {
            return spec;
        }

        return switch (spec) {
            case Key key -> key;
            case Empty empty -> empty;
            case Colorize colorize -> new Colorize(freeze(colorize.child(), busyToo), colorize.rgb());
            case Alpha alpha -> new Alpha(freeze(alpha.child(), busyToo), alpha.alpha());
            case Resize resize -> new Resize(freeze(resize.child(), busyToo), resize.width(), resize.height());
            case Layered layered -> new Layered(layered.children().stream().map(child -> freeze(child, busyToo)).toList());
            case Gray gray -> new Gray(freeze(gray.child(), busyToo), gray.percent());
            case Append append -> new Append(freeze(append.left(), busyToo), freeze(append.right(), busyToo));
            case Text text -> new Text(freeze(text.child(), busyToo), text.text());
            case Busy busy -> busyToo ? busy.still() : busy;
            case Blinking blinking -> freeze(blinking.child(), busyToo);
        };
    }

    static int width(WebImageSpec spec) {
        return switch (spec) {
            case Key key -> key.width();
            case Empty empty -> empty.width();
            case Colorize colorize -> width(colorize.child());
            case Alpha alpha -> width(alpha.child());
            case Resize resize -> resize.width();
            case Layered layered -> layered.children().stream().mapToInt(WebImageSpec::width).max().orElse(0);
            case Gray gray -> width(gray.child());
            // a side of unknown width still takes a box of its own, so the sum is over what is actually drawn
            case Append append -> widthOrDefault(append.left()) + widthOrDefault(append.right());
            case Text text -> width(text.child());
            case Busy busy -> busy.width();
            case Blinking blinking -> width(blinking.child());
        };
    }

    static int height(WebImageSpec spec) {
        return switch (spec) {
            case Key key -> key.height();
            case Empty empty -> empty.height();
            case Colorize colorize -> height(colorize.child());
            case Alpha alpha -> height(alpha.child());
            case Resize resize -> resize.height();
            case Layered layered -> layered.children().stream().mapToInt(WebImageSpec::height).max().orElse(0);
            case Gray gray -> height(gray.child());
            case Append append -> Math.max(height(append.left()), height(append.right()));
            case Text text -> height(text.child());
            case Busy busy -> busy.height();
            case Blinking blinking -> height(blinking.child());
        };
    }

    static int widthOrDefault(WebImageSpec spec) {
        int width = width(spec);
        return width > 0 || spec instanceof Busy ? width : DEFAULT_SIZE;
    }

    static int heightOrDefault(WebImageSpec spec) {
        int height = height(spec);
        return height > 0 || spec instanceof Busy ? height : DEFAULT_SIZE;
    }

    static String encode(WebImageSpec spec) {
        StringBuilder builder = new StringBuilder();
        append(builder, spec);
        return builder.toString();
    }

    private static void append(StringBuilder builder, WebImageSpec spec) {
        switch (spec) {
            case Key key -> builder.append("k(")
                .append(key.groupId()).append(',')
                .append(key.imageId()).append(',')
                .append(key.width()).append(',')
                .append(key.height()).append(')');
            case Empty empty -> builder.append("e(").append(empty.width()).append(',').append(empty.height()).append(')');
            case Colorize colorize -> {
                builder.append("c(");
                append(builder, colorize.child());
                builder.append(',').append(String.format("%06x", colorize.rgb() & 0xFFFFFF)).append(')');
            }
            case Alpha alpha -> {
                builder.append("a(");
                append(builder, alpha.child());
                builder.append(',').append(alpha.alpha()).append(')');
            }
            case Resize resize -> {
                builder.append("r(");
                append(builder, resize.child());
                builder.append(',').append(resize.width()).append(',').append(resize.height()).append(')');
            }
            case Layered layered -> {
                builder.append("l(");
                List<WebImageSpec> children = layered.children();
                for (int i = 0; i < children.size(); i++) {
                    if (i != 0) {
                        builder.append(',');
                    }
                    append(builder, children.get(i));
                }
                builder.append(')');
            }
            case Gray gray -> {
                builder.append("g(");
                append(builder, gray.child());
                builder.append(',').append(gray.percent()).append(')');
            }
            case Append appended -> {
                builder.append("p(");
                append(builder, appended.left());
                builder.append(',');
                append(builder, appended.right());
                builder.append(')');
            }
            case Text text -> {
                builder.append("t(");
                append(builder, text.child());
                builder.append(',').append(encodeText(text.text())).append(')');
            }
            case Busy busy -> builder.append("b(").append(busy.width()).append(',').append(busy.height()).append(')');
            case Blinking blinking -> {
                builder.append("bl(");
                append(builder, blinking.child());
                builder.append(')');
            }
        }
    }

    /**
     * The text is whatever the platform badged an icon with, and the form here delimits on braces and commas
     * without escaping anything - so it travels as base64 rather than as itself.
     */
    private static String encodeText(String text) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(text.getBytes(StandardCharsets.UTF_8));
    }

    private static String decodeText(String text) {
        return new String(Base64.getUrlDecoder().decode(text), StandardCharsets.UTF_8);
    }

    static @Nullable WebImageSpec decode(String text) {
        try {
            Parser parser = new Parser(text);
            WebImageSpec spec = parser.readSpec();
            return parser.atEnd() ? spec : null;
        }
        catch (RuntimeException e) {
            return null;
        }
    }

    final class Parser {
        private final String myText;
        private int myOffset;

        private Parser(String text) {
            myText = text;
        }

        private boolean atEnd() {
            return myOffset == myText.length();
        }

        private WebImageSpec readSpec() {
            String type = readUntil('(');

            switch (type) {
                case "k": {
                    return new Key(readUntil(','), readUntil(','), readInt(','), readInt(')'));
                }
                case "e": {
                    return new Empty(readInt(','), readInt(')'));
                }
                case "c": {
                    WebImageSpec child = readSpec();
                    expect(',');
                    return new Colorize(child, Integer.parseInt(readUntil(')'), 16));
                }
                case "a": {
                    WebImageSpec child = readSpec();
                    expect(',');
                    return new Alpha(child, Float.parseFloat(readUntil(')')));
                }
                case "r": {
                    WebImageSpec child = readSpec();
                    expect(',');
                    return new Resize(child, readInt(','), readInt(')'));
                }
                case "l": {
                    return new Layered(readChildren());
                }
                case "g": {
                    WebImageSpec child = readSpec();
                    expect(',');
                    return new Gray(child, readInt(')'));
                }
                case "p": {
                    WebImageSpec left = readSpec();
                    expect(',');
                    WebImageSpec right = readSpec();
                    expect(')');
                    return new Append(left, right);
                }
                case "t": {
                    WebImageSpec child = readSpec();
                    expect(',');
                    return new Text(child, decodeText(readUntil(')')));
                }
                case "b": {
                    return new Busy(readInt(','), readInt(')'));
                }
                case "bl": {
                    WebImageSpec child = readSpec();
                    expect(')');
                    return new Blinking(child);
                }
                default: {
                    throw new IllegalArgumentException(type);
                }
            }
        }

        private List<WebImageSpec> readChildren() {
            List<WebImageSpec> children = new ArrayList<>();
            while (true) {
                children.add(readSpec());
                char next = myText.charAt(myOffset++);
                if (next == ')') {
                    return children;
                }
                if (next != ',') {
                    throw new IllegalArgumentException(String.valueOf(next));
                }
            }
        }

        /**
         * A nested spec has already eaten its own closing brace, so what follows it is read directly
         * instead of through a separator search that would run past the end of the enclosing spec.
         */
        private void expect(char expected) {
            if (myText.charAt(myOffset++) != expected) {
                throw new IllegalArgumentException(myText);
            }
        }

        private String readUntil(char stop) {
            int index = myText.indexOf(stop, myOffset);
            if (index < 0) {
                throw new IllegalArgumentException(myText);
            }
            String value = myText.substring(myOffset, index);
            myOffset = index + 1;
            return value;
        }

        private int readInt(char stop) {
            return Integer.parseInt(readUntil(stop));
        }
    }
}
