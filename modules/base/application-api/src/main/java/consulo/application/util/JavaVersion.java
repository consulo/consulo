// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.application.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A class representing a version of some Java platform - e.g. the runtime the class is loaded into, or some installed JRE.
 * <p/>
 * Based on <a href="http://openjdk.java.net/jeps/322">JEP 322 "Time-Based Release Versioning"</a> (Java 10+), but also supports JEP 223
 * "New Version-String Scheme" (Java 9), as well as earlier version's formats.
 *
 * @see #parse(String) for examples of supported version strings
 */
public final class JavaVersion implements Comparable<JavaVersion> {
    /**
     * The major version.
     * Corresponds to the first number of a Java 9+ version string and to the second number of Java 1.0 to 1.8 strings.
     */
    public final int feature;

    /**
     * The minor version.
     * Corresponds to the second number of a Java 9+ version string and to the third number of Java 1.0 to 1.8 strings.
     * Used in version strings prior to Java 1.5, in newer strings is always {@code 0}.
     */
    public final int minor;

    /**
     * The patch version.
     * Corresponds to the third number of a Java 9+ version string and to the number of Java 1.0 to 1.8 strings (one after an underscore).
     */
    public final int update;

    /**
     * The build number.
     * Corresponds to a number prefixed by a plus sign in a Java 9+ version string and by "-b" string in earlier versions.
     */
    public final int build;

    /**
     * {@code true} if the platform is an early access release, {@code false} otherwise (or when not known).
     */
    public final boolean ea;

    private JavaVersion(int feature, int minor, int update, int build, boolean ea) {
        this.feature = feature;
        this.minor = minor;
        this.update = update;
        this.build = build;
        this.ea = ea;
    }

    @Override
    public int compareTo(JavaVersion o) {
        int diff = feature - o.feature;
        if (diff != 0) {
            return diff;
        }
        diff = minor - o.minor;
        if (diff != 0) {
            return diff;
        }
        diff = update - o.update;
        if (diff != 0) {
            return diff;
        }
        diff = build - o.build;
        if (diff != 0) {
            return diff;
        }
        return (ea ? 0 : 1) - (o.ea ? 0 : 1);
    }

    public boolean isAtLeast(int feature) {
        return this.feature >= feature;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof JavaVersion)) {
            return false;
        }
        JavaVersion other = (JavaVersion) o;
        return feature == other.feature && minor == other.minor && update == other.update && build == other.build && ea == other.ea;
    }

    @Override
    public int hashCode() {
        int hash = feature;
        hash = 31 * hash + minor;
        hash = 31 * hash + update;
        hash = 31 * hash + build;
        hash = 31 * hash + (ea ? 1231 : 1237);
        return hash;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (feature > 8) {
            sb.append(feature);
            if (minor > 0 || update > 0) {
                sb.append('.').append(minor);
            }
            if (update > 0) {
                sb.append('.').append(update);
            }
            if (ea) {
                sb.append("-ea");
            }
            if (build > 0) {
                sb.append('+').append(build);
            }
        }
        else {
            sb.append("1.").append(feature);
            if (minor > 0 || update > 0 || ea || build > 0) {
                sb.append('.').append(minor);
            }
            if (update > 0) {
                sb.append('_').append(update);
            }
            if (ea) {
                sb.append("-ea");
            }
            if (build > 0) {
                sb.append("-b").append(build);
            }
        }
        return sb.toString();
    }

    /**
     * Composes a version object out of given parameters.
     *
     * @throws IllegalArgumentException when any of numbers is negative
     */
    public static JavaVersion compose(int feature, int minor, int update, int build, boolean ea) throws IllegalArgumentException {
        if (feature < 0) {
            throw new IllegalArgumentException();
        }
        if (minor < 0) {
            throw new IllegalArgumentException();
        }
        if (update < 0) {
            throw new IllegalArgumentException();
        }
        if (build < 0) {
            throw new IllegalArgumentException();
        }
        return new JavaVersion(feature, minor, update, build, ea);
    }

    public static JavaVersion compose(int feature) {
        return compose(feature, 0, 0, 0, false);
    }

    private static JavaVersion current;

    /**
     * Returns the version of a Java runtime the class is loaded into.
     * The method attempts to parse {@code "java.runtime.version"} system property first (usually, it is more complete),
     * and falls back to {@code "java.version"} if the former is invalid or differs in {@link #feature} or {@link #minor} numbers.
     */

    public static JavaVersion current() {
        if (current == null) {
            JavaVersion fallback = parse(System.getProperty("java.version"));
            JavaVersion rt = rtVersion();
            if (rt == null) {
                try {
                    rt = parse(System.getProperty("java.runtime.version"));
                }
                catch (Throwable ignored) {
                }
            }
            current = rt != null && rt.feature == fallback.feature && rt.minor == fallback.minor ? rt : fallback;
        }
        return current;
    }

    private static JavaVersion rtVersion() {
        try {
            Runtime.Version version = Runtime.version();
            int major = version.major();
            int minor = version.minor();
            int security = version.security();
            Optional<Integer> buildOpt = version.build();
            int build = buildOpt.orElse(0);
            Optional<String> preOpt = version.pre();
            boolean ea = preOpt.isPresent();
            return new JavaVersion(major, minor, security, build, ea);
        }
        catch (Throwable ignored) {
            return null;
        }
    }

    private static final int MAX_ACCEPTED_VERSION = 50;  // sanity check

    /**
     * <p>Parses a Java version string.</p>
     * <p/>
     * <p>Supports various sources, including (but not limited to):<br>
     * - {@code "java.*version"} system properties (a version number without any decoration)<br>
     * - values of Java compiler -source/-target/--release options ("$MAJOR", "1.$MAJOR")</br>
     * - output of "{@code java -version}" (usually "java version \"$VERSION\"")<br>
     * - a second line of the above command (something like to "Java(TM) SE Runtime Environment (build $VERSION)")<br>
     * - output of "{@code java --full-version}" ("java $VERSION")</p>
     * <p/>
     * <p>See consulo.ide.impl.idea.util.lang.JavaVersionTest for examples.</p>
     *
     * @throws IllegalArgumentException if failed to recognize the number.
     */
    public static JavaVersion parse(String versionString) throws IllegalArgumentException {
        String str = versionString.trim();
        Map<String, String> trimmingMap = new LinkedHashMap<>();
        trimmingMap.put("Runtime Environment", "(build ");
        trimmingMap.put("OpenJ9", "version ");
        trimmingMap.put("GraalVM", "Java ");
        for (Map.Entry<String, String> entry : trimmingMap.entrySet()) {
            String key = entry.getKey();
            if (str.contains(key)) {
                int p = str.indexOf(entry.getValue());
                if (p > 0) {
                    str = str.substring(p);
                }
                break;
            }
        }

        List<String> numbers = new ArrayList<>();
        List<String> separators = new ArrayList<>();
        int length = str.length();
        int p = 0;
        boolean number = Character.isDigit(str.charAt(0));
        while (p < length) {
            int start = p;
            while (p < length && Character.isDigit(str.charAt(p)) == number) {
                p++;
            }
            String part = str.substring(start, p);
            if (number) {
                numbers.add(part);
            }
            else {
                separators.add(part);
            }
            number = !number;
        }

        if (!numbers.isEmpty() && !separators.isEmpty()) {
            try {
                int feature = Integer.parseInt(numbers.get(0));
                int minor = 0;
                int update = 0;
                int build = 0;
                boolean ea = false;

                if (feature >= 5 && feature < MAX_ACCEPTED_VERSION) {
                    p = 1;
                    while (p < separators.size() && ".".equals(separators.get(p))) p++;
                    if (p > 1 && numbers.size() > 2) {
                        minor = Integer.parseInt(numbers.get(1));
                        update = Integer.parseInt(numbers.get(2));
                    }
                    if (p < separators.size()) {
                        String s = separators.get(p);
                        if (!s.isEmpty() && s.charAt(0) == '-') {
                            ea = startsWithWord(s, "-ea") || startsWithWord(s, "-internal");
                            if (p < numbers.size() && s.charAt(s.length() - 1) == '+') {
                                build = Integer.parseInt(numbers.get(p));
                            }
                        }
                        if (build == 0 && p + 1 < separators.size() && "+".equals(separators.get(p + 1))) {
                            build = Integer.parseInt(numbers.get(p + 1));
                        }
                    }
                    return new JavaVersion(feature, minor, update, build, ea);
                }
                else if (feature == 1 && numbers.size() > 1 && separators.size() > 1
                    && ".".equals(separators.get(1))) {
                    feature = Integer.parseInt(numbers.get(1));
                    if (feature <= MAX_ACCEPTED_VERSION) {
                        if (numbers.size() > 2 && separators.size() > 2 && ".".equals(separators.get(2))) {
                            minor = Integer.parseInt(numbers.get(2));
                            if (numbers.size() > 3 && separators.size() > 3 && "_".equals(separators.get(3))) {
                                update = Integer.parseInt(numbers.get(3));
                                if (separators.size() > 4) {
                                    String s = separators.get(4);
                                    if (!s.isEmpty() && s.charAt(0) == '-') {
                                        ea = startsWithWord(s, "-ea") || startsWithWord(s, "-internal");
                                    }
                                    int q = 4;
                                    while (q < separators.size() && !separators.get(q).endsWith("-b")) {
                                        q++;
                                    }
                                    if (q < numbers.size()) {
                                        build = Integer.parseInt(numbers.get(q));
                                    }
                                }
                            }
                        }
                        return new JavaVersion(feature, minor, update, build, ea);
                    }
                }
            }
            catch (NumberFormatException ignored) {
            }
        }

        throw new IllegalArgumentException("Invalid version: " + versionString);
    }

    private static boolean startsWithWord(String s, String word) {
        return s.startsWith(word) && (s.length() == word.length() || !Character.isLetterOrDigit(s.charAt(word.length())));
    }

    /**
     * A safe version of {@link #parse(String)} - returns {@code null} if can't parse a version string.
     */
    public static JavaVersion tryParse(String versionString) {
        if (versionString != null) {
            try {
                return parse(versionString);
            }
            catch (IllegalArgumentException ignored) {
            }
        }

        return null;
    }
}