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
package consulo.web.internal.ui.image;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
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

    static int width(WebImageSpec spec) {
        return switch (spec) {
            case Key key -> key.width();
            case Empty empty -> empty.width();
            case Colorize colorize -> width(colorize.child());
            case Alpha alpha -> width(alpha.child());
            case Resize resize -> resize.width();
            case Layered layered -> layered.children().stream().mapToInt(WebImageSpec::width).max().orElse(0);
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
        };
    }

    static int widthOrDefault(WebImageSpec spec) {
        int width = width(spec);
        return width > 0 ? width : DEFAULT_SIZE;
    }

    static int heightOrDefault(WebImageSpec spec) {
        int height = height(spec);
        return height > 0 ? height : DEFAULT_SIZE;
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
        }
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
