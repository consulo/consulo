package consulo.language.editor.internal;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.ApplicationManager;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.language.Language;
import jakarta.inject.Singleton;
import org.jdom.Element;

import java.util.*;

@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
@Singleton
@State(name = "ParameterNameHintsSettings", storages = {@Storage("parameter.hints.xml")})
public class ParameterNameHintsSettings implements PersistentStateComponent<Element> {
    public static ParameterNameHintsSettings getInstance() {
        return ApplicationManager.getApplication().getInstance(ParameterNameHintsSettings.class);
    }

    static final String BLACKLISTS = "blacklists";
    static final String LANGUAGE_LIST = "blacklist";
    static final String LANGUAGE = "language";
    static final String ADDED = "added";
    static final String REMOVED = "removed";
    static final String PATTERN = "pattern";
    static final String DISABLED_LANGUAGES = "disabledLanguages";
    static final String DISABLED_LANGUAGE_ITEM = "language";
    static final String DISABLED_LANGUAGE_ID = "id";

    /**
     * Tracks additions and removals in blacklist patterns
     */
    public static class Diff {
        public final Set<String> added;
        public final Set<String> removed;

        public Diff(Set<String> added, Set<String> removed) {
            this.added = added;
            this.removed = removed;
        }

        public Set<String> applyOn(Set<String> base) {
            Set<String> result = new HashSet<>(base);
            result.addAll(added);
            result.removeAll(removed);
            return result;
        }

        public static Diff build(Set<String> base, Set<String> updated) {
            Set<String> removed = new HashSet<>(base);
            removed.removeAll(updated);
            Set<String> added = new HashSet<>(updated);
            added.removeAll(base);
            return new Diff(added, removed);
        }
    }

    private final Map<String, Set<String>> removedPatterns = new HashMap<>();
    private final Map<String, Set<String>> addedPatterns = new HashMap<>();
    private final Map<String, Boolean> options = new HashMap<>();
    private final Set<String> disabledLanguages = new HashSet<>();

    public void addIgnorePattern(Language language, String pattern) {
        Set<String> before = getAddedPatterns(language);
        Set<String> updated = new HashSet<>(before);
        updated.add(pattern);
        setAddedPatterns(language, updated);
    }

    public Diff getExcludeListDiff(Language language) {
        return Diff.build(getAddedPatterns(language), getRemovedPatterns(language));
    }

    public void setExcludeListDiff(Language language, Diff diff) {
        setAddedPatterns(language, diff.added);
        setRemovedPatterns(language, diff.removed);
    }

    @Override
    public Element getState() {
        Element root = new Element("settings");

        if (!removedPatterns.isEmpty() || !addedPatterns.isEmpty()) {
            Element blacklists = new Element(BLACKLISTS);
            root.addContent(blacklists);
            Set<String> langs = new HashSet<>();
            langs.addAll(removedPatterns.keySet());
            langs.addAll(addedPatterns.keySet());
            for (String lang : langs) {
                Element listEl = new Element(LANGUAGE_LIST);
                listEl.setAttribute(LANGUAGE, lang);
                for (String s : addedPatterns.getOrDefault(lang, Collections.emptySet())) {
                    listEl.addContent(toPatternElement(s, ADDED));
                }
                for (String s : removedPatterns.getOrDefault(lang, Collections.emptySet())) {
                    listEl.addContent(toPatternElement(s, REMOVED));
                }
                blacklists.addContent(listEl);
            }
        }

        for (Map.Entry<String, Boolean> entry : options.entrySet()) {
            Element opt = new Element("option");
            opt.setAttribute("id", entry.getKey());
            opt.setAttribute("value", entry.getValue().toString());
            root.addContent(opt);
        }

        if (!disabledLanguages.isEmpty()) {
            Element disabledEl = new Element(DISABLED_LANGUAGES);
            for (String id : disabledLanguages) {
                Element langEl = new Element(DISABLED_LANGUAGE_ITEM);
                langEl.setAttribute(DISABLED_LANGUAGE_ID, id);
                disabledEl.addContent(langEl);
            }
            root.addContent(disabledEl);
        }

        return root;
    }

    @Override
    public void loadState(Element state) {
        removedPatterns.clear();
        addedPatterns.clear();
        options.clear();
        disabledLanguages.clear();

        Element blacks = state.getChild(BLACKLISTS);
        if (blacks != null) {
            for (Element listEl : blacks.getChildren(LANGUAGE_LIST)) {
                String lang = listEl.getAttributeValue(LANGUAGE);
                if (lang == null) {
                    continue;
                }
                Set<String> added = extractPatterns(listEl, ADDED);
                Set<String> removed = extractPatterns(listEl, REMOVED);
                addedPatterns.put(lang, added);
                removedPatterns.put(lang, removed);
            }
        }

        for (Element optEl : state.getChildren("option")) {
            String id = optEl.getAttributeValue("id");
            boolean val = Boolean.parseBoolean(optEl.getAttributeValue("value"));
            options.put(id, val);
        }

        Element disabledEl = state.getChild(DISABLED_LANGUAGES);
        if (disabledEl != null) {
            for (Element langEl : disabledEl.getChildren(DISABLED_LANGUAGE_ITEM)) {
                String id = langEl.getAttributeValue(DISABLED_LANGUAGE_ID);
                if (id != null) {
                    disabledLanguages.add(id);
                }
            }
        }
    }

    public void setEnabledForLanguage(boolean enabled, Language language) {
        String key = language.getID();
        if (!enabled) {
            disabledLanguages.add(key);
        }
        else {
            disabledLanguages.remove(key);
        }
    }

    public boolean isEnabledForLanguage(Language language) {
        return !disabledLanguages.contains(language.getID());
    }

    public Boolean getOption(String optionId) {
        return options.get(optionId);
    }

    public void setOption(String optionId, Boolean value) {
        if (value == null) {
            options.remove(optionId);
        }
        else {
            options.put(optionId, value);
        }
    }

    private Set<String> getAddedPatterns(Language language) {
        return addedPatterns.getOrDefault(language.getID(), Collections.emptySet());
    }

    private Set<String> getRemovedPatterns(Language language) {
        return removedPatterns.getOrDefault(language.getID(), Collections.emptySet());
    }

    private void setAddedPatterns(Language language, Set<String> patterns) {
        addedPatterns.put(language.getID(), patterns);
    }

    private void setRemovedPatterns(Language language, Set<String> patterns) {
        removedPatterns.put(language.getID(), patterns);
    }

    private static Set<String> extractPatterns(Element parent, String tag) {
        Set<String> result = new HashSet<>();
        for (Element e : parent.getChildren(tag)) {
            String p = e.getAttributeValue(PATTERN);
            if (p != null) {
                result.add(p);
            }
        }
        return result;
    }

    private static Element toPatternElement(String pattern, String tag) {
        Element e = new Element(tag);
        e.setAttribute(PATTERN, pattern);
        return e;
    }
}
