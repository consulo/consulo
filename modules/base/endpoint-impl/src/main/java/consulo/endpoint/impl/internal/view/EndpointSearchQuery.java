package consulo.endpoint.impl.internal.view;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class EndpointSearchQuery {
    public static final String TYPE_TAG = "type";
    public static final String FRAMEWORK_TAG = "framework";
    public static final String EXTERNAL_MODULE = "external";

    private final List<String> myTypes;
    private final List<String> myFrameworks;
    private final List<String> myModules;
    private final List<String> myWords;

    private EndpointSearchQuery(List<String> types, List<String> frameworks, List<String> modules, List<String> words) {
        myTypes = types;
        myFrameworks = frameworks;
        myModules = modules;
        myWords = words;
    }

    public static EndpointSearchQuery parse(String text, String moduleTag) {
        List<String> types = new ArrayList<>();
        List<String> frameworks = new ArrayList<>();
        List<String> modules = new ArrayList<>();
        List<String> words = new ArrayList<>();

        String moduleKey = moduleTag.toLowerCase(Locale.ROOT);
        for (String token : tokenize(text)) {
            int colon = token.indexOf(':');
            if (colon > 0) {
                String key = token.substring(0, colon).toLowerCase(Locale.ROOT);
                String value = token.substring(colon + 1).toLowerCase(Locale.ROOT);
                List<String> target = null;
                if (key.equals(TYPE_TAG)) {
                    target = types;
                }
                else if (key.equals(FRAMEWORK_TAG)) {
                    target = frameworks;
                }
                else if (key.equals(moduleKey)) {
                    target = modules;
                }

                if (target != null) {
                    if (!value.isEmpty()) {
                        target.add(value);
                    }
                    continue;
                }
            }
            words.add(token.toLowerCase(Locale.ROOT));
        }
        return new EndpointSearchQuery(types, frameworks, modules, words);
    }

    public static String quote(String value) {
        String text = value.replace("\"", "");
        for (int i = 0; i < text.length(); i++) {
            if (Character.isWhitespace(text.charAt(i))) {
                return "\"" + text + "\"";
            }
        }
        return text;
    }

    private static List<String> tokenize(String text) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '"') {
                quoted = !quoted;
            }
            else if (Character.isWhitespace(c) && !quoted) {
                if (!current.isEmpty()) {
                    tokens.add(current.toString());
                    current.setLength(0);
                }
            }
            else {
                current.append(c);
            }
        }
        if (!current.isEmpty()) {
            tokens.add(current.toString());
        }
        return tokens;
    }

    public boolean matchesModule(EndpointModuleSnapshot section) {
        if (myModules.isEmpty()) {
            return true;
        }

        String name = section.isExternal() ? EXTERNAL_MODULE : section.getKey().toLowerCase(Locale.ROOT);
        return myModules.contains(name) || myModules.contains(section.getName().get().toLowerCase(Locale.ROOT));
    }

    public boolean matches(EndpointRowData<?, ?> data) {
        if (!myTypes.isEmpty()
            && !myTypes.contains(data.getTypeTag().toLowerCase(Locale.ROOT))
            && !myTypes.contains(data.getTypeText().get().toLowerCase(Locale.ROOT))) {
            return false;
        }

        if (!myFrameworks.isEmpty()
            && !myFrameworks.contains(data.getFrameworkTag().toLowerCase(Locale.ROOT))
            && !myFrameworks.contains(data.getFrameworkTitle().toLowerCase(Locale.ROOT))) {
            return false;
        }

        String searchText = data.getSearchText();
        for (String word : myWords) {
            if (!searchText.contains(word)) {
                return false;
            }
        }
        return true;
    }
}
