// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.url;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

public final class UrlPath {
    public static final UrlPath EMPTY = new UrlPath(List.of(new PathSegment.Exact("")));

    static final String STAR = "*";
    private static final String UNKNOWN = "<???>";

    public static final String UNKNOWN_URL_PATH_SEGMENT_PRESENTATION = "${..}";

    public static final PathSegmentRenderer DEFAULT_PATH_VARIABLE_PRESENTATION = new PathSegmentRenderer() {
        @Override
        public String visitVariable(PathSegment.Variable variable) {
            return STAR;
        }
    };

    public static final PathSegmentRenderer FULL_PATH_VARIABLE_PRESENTATION = new PathSegmentRenderer() {
        @Override
        public String visitVariable(PathSegment.Variable variable) {
            StringBuilder builder = new StringBuilder();
            @Nullable String variableName = variable.getVariableName();
            if (variableName == null || variableName.isEmpty()) {
                builder.append(STAR);
            }
            else {
                builder.append("{");
                builder.append(variableName);
                builder.append("}");
            }
            return builder.toString();
        }
    };

    private final List<PathSegment> mySegments;

    public UrlPath(List<? extends PathSegment> segments) {
        mySegments = Collections.unmodifiableList(segments);
    }

    public List<PathSegment> getSegments() {
        return mySegments;
    }

    public static sealed abstract class PathSegment
        permits PathSegment.Exact, PathSegment.Variable, PathSegment.Composite, PathSegment.Undefined {

        private PathSegment() {
        }

        public @Nullable String getValueIfExact() {
            return this instanceof Exact exact ? exact.getValue() : null;
        }

        public boolean isEmpty() {
            @Nullable String value = getValueIfExact();
            return value != null && value.isEmpty();
        }

        public static final class Exact extends PathSegment {
            private final String myValue;

            public Exact(String value) {
                myValue = value;
            }

            public String getValue() {
                return myValue;
            }

            @Override
            public boolean equals(@Nullable Object other) {
                return other instanceof Exact exact && exact.myValue.equals(myValue);
            }

            @Override
            public int hashCode() {
                return myValue.hashCode();
            }

            @Override
            public String toString() {
                return "PathSegment.Exact(" + myValue + ")";
            }
        }

        public static final class Variable extends PathSegment {
            private final @Nullable String myVariableName;
            private final @Nullable String myRegex;

            public Variable(@Nullable String variableName) {
                this(variableName, null);
            }

            public Variable(@Nullable String variableName, @Nullable String regex) {
                myVariableName = variableName;
                myRegex = regex;
            }

            public @Nullable String getVariableName() {
                return myVariableName;
            }

            public @Nullable String getRegex() {
                return myRegex;
            }

            @Override
            public boolean equals(@Nullable Object other) {
                return other instanceof Variable variable && Objects.equals(variable.myVariableName, myVariableName);
            }

            @Override
            public int hashCode() {
                return Objects.hashCode(myVariableName);
            }

            @Override
            public String toString() {
                return "PathSegment.Variable({" + myVariableName + "})";
            }

            public boolean accepts(String value) {
                return true;
            }
        }

        public static final class Composite extends PathSegment {
            private final List<PathSegment> mySegments;

            public Composite(List<? extends PathSegment> segments) {
                mySegments = Collections.unmodifiableList(segments);
            }

            public List<PathSegment> getSegments() {
                return mySegments;
            }

            @Override
            public boolean equals(@Nullable Object other) {
                return other instanceof Composite composite && composite.mySegments.equals(mySegments);
            }

            @Override
            public int hashCode() {
                return mySegments.hashCode();
            }

            @Override
            public String toString() {
                return "PathSegment.Composite(" + mySegments + ")";
            }
        }

        public static final class Undefined extends PathSegment {
            public static final Undefined INSTANCE = new Undefined();

            private Undefined() {
            }
        }
    }

    public String getPresentation() {
        return getPresentation(DEFAULT_PATH_VARIABLE_PRESENTATION);
    }

    public String getPresentation(PathSegmentRenderer pathVariableRenderer) {
        StringJoiner joiner = new StringJoiner("/");
        for (PathSegment segment : mySegments) {
            joiner.add(pathVariableRenderer.patternMatch(segment));
        }
        return joiner.toString();
    }

    public String toStringWithStars() {
        StringJoiner joiner = new StringJoiner("/");
        for (PathSegment segment : mySegments) {
            if (segment instanceof PathSegment.Exact exact) {
                joiner.add(exact.getValue());
            }
            else if (segment instanceof PathSegment.Variable) {
                joiner.add(STAR);
            }
            else if (segment instanceof PathSegment.Composite) {
                joiner.add(STAR);
            }
            else {
                joiner.add(UNKNOWN);
            }
        }
        return joiner.toString();
    }

    @Override
    public String toString() {
        return "UrlPath(" + toStringWithStars() + ")";
    }

    public boolean canBePrefixFor(UrlPath another) {
        return commonLength(another) == mySegments.size();
    }

    public int commonLength(UrlPath another) {
        List<PathSegment> cur = mySegments;
        int start = 0;
        int accum = 0;
        while (true) {
            if (cur.isEmpty()) {
                return accum;
            }
            PathSegment head = cur.get(0);
            List<PathSegment> tail = cur.subList(1, cur.size());
            List<PathSegment> remaining = another.mySegments.subList(start, another.mySegments.size());
            if (head instanceof PathSegment.Exact exactHead) {
                int anotherExactIndex = -1;
                for (int i = 0; i < remaining.size(); i++) {
                    if (remaining.get(i) instanceof PathSegment.Exact) {
                        anotherExactIndex = i;
                        break;
                    }
                }
                if (anotherExactIndex == -1) {
                    return accum;
                }
                PathSegment.Exact anotherExact = (PathSegment.Exact) remaining.get(anotherExactIndex);
                @Nullable PathSegment first = remaining.isEmpty() ? null : remaining.get(0);
                if (anotherExact.equals(exactHead)) {
                    cur = tail;
                    start = start + anotherExactIndex + 1;
                    accum = accum + 1;
                }
                else if (exactHead.getValue().isEmpty()) {
                    cur = tail;
                    accum = accum + 1;
                }
                else if (first instanceof PathSegment.Variable variable && variable.accepts(exactHead.getValue())) {
                    cur = tail;
                    start = start + 1;
                    accum = accum + 1;
                }
                else if (first instanceof PathSegment.Undefined) {
                    cur = tail;
                    start = start + 1;
                    accum = accum + 1;
                }
                else {
                    return accum;
                }
            }
            else {
                if (remaining.isEmpty()) {
                    return accum;
                }
                PathSegment aHead = remaining.get(0);
                if (!(aHead instanceof PathSegment.Exact)) {
                    cur = tail;
                    start = start + 1;
                    accum = accum + 1;
                }
                else {
                    cur = tail;
                    accum = accum + 1;
                }
            }
        }
    }

    private static boolean isEmptyExact(PathSegment headA) {
        return headA instanceof PathSegment.Exact exact && exact.getValue().equals("");
    }

    private static boolean hasValue(List<PathSegment> another) {
        for (PathSegment segment : another) {
            if (!(isEmptyExact(segment) || segment instanceof PathSegment.Undefined)) {
                return true;
            }
        }
        return false;
    }

    public boolean isCompatibleWith(UrlPath another) {
        List<PathSegment> cur = mySegments;
        List<PathSegment> other = another.mySegments;
        while (true) {
            if (cur.isEmpty() && !hasValue(other)) {
                return true;
            }
            if (other.isEmpty() && !hasValue(cur)) {
                return true;
            }
            if (cur.isEmpty() && other.isEmpty()) {
                return true;
            }
            if (cur.isEmpty() || other.isEmpty()) {
                return false;
            }
            PathSegment headA = cur.get(0);
            List<PathSegment> tailA = cur.subList(1, cur.size());
            PathSegment headB = other.get(0);
            List<PathSegment> tailB = other.subList(1, other.size());
            if (isEmptyExact(headA)) {
                List<PathSegment> nextOther = isEmptyExact(headB) ? tailB : other;
                cur = tailA;
                other = nextOther;
            }
            else if (isEmptyExact(headB)) {
                List<PathSegment> nextCur = isEmptyExact(headA) ? tailA : cur;
                cur = nextCur;
                other = tailB;
            }
            else if (headA instanceof PathSegment.Undefined && headB instanceof PathSegment.Undefined) {
                cur = tailA;
                other = tailB;
            }
            else if (headA instanceof PathSegment.Undefined && hasValue(tailA)
                || headB instanceof PathSegment.Undefined && hasValue(tailB)) {
                cur = tailA;
                other = tailB;
            }
            else if (headA instanceof PathSegment.Undefined && !hasValue(tailA)) {
                return true;
            }
            else if (headB instanceof PathSegment.Undefined && !hasValue(tailB)) {
                return true;
            }
            else if (headA instanceof PathSegment.Variable && headB instanceof PathSegment.Variable) {
                cur = tailA;
                other = tailB;
            }
            else if (headA instanceof PathSegment.Variable variableA && headB instanceof PathSegment.Exact exactB) {
                if (!variableA.accepts(exactB.getValue())) {
                    return false;
                }
                cur = tailA;
                other = tailB;
            }
            else if (headA instanceof PathSegment.Exact exactA && headB instanceof PathSegment.Variable variableB) {
                if (!variableB.accepts(exactA.getValue())) {
                    return false;
                }
                cur = tailA;
                other = tailB;
            }
            else if (headA instanceof PathSegment.Exact exactA && headB instanceof PathSegment.Exact exactB) {
                if (!exactA.getValue().equals(exactB.getValue())) {
                    return false;
                }
                cur = tailA;
                other = tailB;
            }
            else if (headA instanceof PathSegment.Composite compositeA && headB instanceof PathSegment.Composite compositeB) {
                if (!new UrlPath(compositeA.getSegments()).isCompatibleWith(new UrlPath(compositeB.getSegments()))) {
                    return false;
                }
                cur = tailA;
                other = tailB;
            }
            else {
                return false;
            }
        }
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        UrlPath urlPath = (UrlPath) other;
        return mySegments.equals(urlPath.mySegments);
    }

    @Override
    public int hashCode() {
        return mySegments.hashCode();
    }

    public static UrlPath fromExactString(String string) {
        String[] parts = string.split("/", -1);
        List<PathSegment> segments = new ArrayList<>(parts.length);
        for (String part : parts) {
            segments.add(new PathSegment.Exact(part));
        }
        return new UrlPath(segments);
    }

    public static List<UrlPath> combinations(UrlPath urlPath) {
        List<UrlPath> result = new ArrayList<>();
        collectCombinations(urlPath, result);
        return result;
    }

    private static void collectCombinations(UrlPath urlPath, List<UrlPath> result) {
        result.add(urlPath);

        List<PathSegment> segments = urlPath.getSegments();
        if (!segments.isEmpty() && segments.get(0).isEmpty()) {
            result.add(new UrlPath(segments.subList(1, segments.size())));
        }

        int undefined = segments.indexOf(PathSegment.Undefined.INSTANCE);
        if (undefined == -1) {
            return;
        }

        List<PathSegment> joined = new ArrayList<>(segments.subList(0, undefined));
        joined.addAll(segments.subList(undefined + 1, segments.size()));
        collectCombinations(new UrlPath(joined), result);
    }
}
