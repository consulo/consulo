/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.language.codeStyle.arrangement.std;

import consulo.language.codeStyle.CodeStyleBundle;
import consulo.util.lang.lazy.LazyValue;
import org.jetbrains.annotations.PropertyKey;

import org.jspecify.annotations.Nullable;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import static consulo.language.codeStyle.arrangement.std.StdArrangementTokens.EntryType.METHOD;

/**
 * Holds settings tokens used by built-in IJ arrangers.
 *
 * @author Denis Zhdanov
 * @since 3/6/13 3:16 PM
 */
public class StdArrangementTokens {

  private static final Map<String, StdArrangementSettingsToken> TOKENS_BY_ID = new HashMap<>();

  /**
   * Forces nested classes initialization - otherwise it's possible that, say, {@link #byId(String)} returns null for valid
   * id just because corresponding nested class hasn't been initialized yet.
   */
  private static final Supplier<Integer> NESTED_CLASSES_INITIALIZER = LazyValue.notNull(() -> {
    int dummy = 0;
    for (Class<?> clazz : StdArrangementTokens.class.getClasses()) {
      try {
        dummy += clazz.getDeclaredFields()[0].get(null).hashCode();
      }
      catch (IllegalAccessException e) {
        assert false;
      }
    }
    return dummy;
  });

  private StdArrangementTokens() {
  }

  @SuppressWarnings("ReturnValueIgnored") // initializing nested classes
  public static @Nullable ArrangementSettingsToken byId(String id) {
    NESTED_CLASSES_INITIALIZER.get();
    return TOKENS_BY_ID.get(id);
  }

  private static Supplier<Set<ArrangementSettingsToken>> collectFields(Class<?> clazz) {
    return LazyValue.notNull(() -> {
      Set<ArrangementSettingsToken> result = new HashSet<>();
      for (Field field : clazz.getFields()) {
        if (ArrangementSettingsToken.class.isAssignableFrom(field.getType())) {
          try {
            result.add((ArrangementSettingsToken)field.get(null));
          }
          catch (IllegalAccessException e) {
            assert false : e;
          }
        }
      }
      return result;
    });
  }

  private static StdArrangementSettingsToken invertible(String id, StdArrangementTokenType type) {
    StdArrangementSettingsToken result = StdInvertibleArrangementSettingsToken.invertibleTokenById(id, type);
    TOKENS_BY_ID.put(id, result);
    return result;
  }

  private static StdArrangementSettingsToken token(String id, StdArrangementTokenType type) {
    StdArrangementSettingsToken result = StdArrangementSettingsToken.tokenById(id, type);
    TOKENS_BY_ID.put(id, result);
    return result;
  }

  private static StdArrangementSettingsToken compositeToken(String id, StdArrangementTokenType type, ArrangementSettingsToken... alternativeTokens) {
    StdArrangementSettingsToken result = CompositeArrangementToken.create(id, type, alternativeTokens);
    TOKENS_BY_ID.put(id, result);
    return result;
  }

  private static StdArrangementSettingsToken token(String id, @PropertyKey(resourceBundle = CodeStyleBundle.BUNDLE) String key, StdArrangementTokenType type) {
    StdArrangementSettingsToken result = StdArrangementSettingsToken.tokenByBundle(id, key, type);
    TOKENS_BY_ID.put(id, result);
    return result;
  }

  public static class General {
    
    public static final ArrangementSettingsToken TYPE = token("TYPE", "arrangement.settings.text.general.type", StdArrangementTokenType.GENERAL);
    
    public static final ArrangementSettingsToken MODIFIER = token("MODIFIER", "arrangement.settings.text.general.modifier", StdArrangementTokenType.GENERAL);
    
    public static final ArrangementSettingsToken ORDER = token("ORDER", "arrangement.settings.text.general.order", StdArrangementTokenType.GENERAL);
    
    public static final ArrangementSettingsToken ALIAS = token("ALIAS", "arrangement.settings.text.general.sequence", StdArrangementTokenType.GENERAL);

    private General() {
    }
  }

  public static class Section {
    
    public static final ArrangementSettingsToken START_SECTION = token("SECTION_START", StdArrangementTokenType.ENTRY_TYPE);
    
    public static final ArrangementSettingsToken END_SECTION = token("SECTION_END", StdArrangementTokenType.ENTRY_TYPE);

    private Section() {

    }
  }

  public static class Regexp {
    
    public static final StdArrangementSettingsToken NAME = token("NAME", "arrangement.settings.text.general.name", StdArrangementTokenType.REG_EXP);

    
    public static final StdArrangementSettingsToken XML_NAMESPACE = token("XML_NAMESPACE", "arrangement.settings.text.general.xml.namespace", StdArrangementTokenType.REG_EXP);

    
    public static final StdArrangementSettingsToken TEXT = token("TEXT", "arrangement.settings.text.general.text", StdArrangementTokenType.REG_EXP);

    private Regexp() {
    }
  }

  public static class EntryType {
    
    public static final ArrangementSettingsToken CLASS = invertible("CLASS", StdArrangementTokenType.ENTRY_TYPE);
    
    public static final ArrangementSettingsToken ANONYMOUS_CLASS = invertible("ANONYMOUS_CLASS", StdArrangementTokenType.ENTRY_TYPE);
    
    public static final ArrangementSettingsToken FIELD = invertible("FIELD", StdArrangementTokenType.ENTRY_TYPE);
    
    public static final ArrangementSettingsToken CONSTRUCTOR = invertible("CONSTRUCTOR", StdArrangementTokenType.ENTRY_TYPE);
    
    public static final ArrangementSettingsToken METHOD = invertible("METHOD", StdArrangementTokenType.ENTRY_TYPE);
    
    public static final ArrangementSettingsToken ENUM = invertible("ENUM", StdArrangementTokenType.ENTRY_TYPE);
    
    public static final ArrangementSettingsToken INTERFACE = invertible("INTERFACE", StdArrangementTokenType.ENTRY_TYPE);
    
    public static final ArrangementSettingsToken CONST = invertible("CONST", StdArrangementTokenType.ENTRY_TYPE);
    
    public static final ArrangementSettingsToken VAR = invertible("VAR", StdArrangementTokenType.ENTRY_TYPE);
    
    public static final ArrangementSettingsToken PROPERTY = invertible("PROPERTY", StdArrangementTokenType.ENTRY_TYPE);
    
    public static final ArrangementSettingsToken EVENT_HANDLER = invertible("EVENT_HANDLER", StdArrangementTokenType.ENTRY_TYPE);
    
    public static final ArrangementSettingsToken STATIC_INIT = invertible("STATIC_INIT", StdArrangementTokenType.ENTRY_TYPE);
    
    public static final ArrangementSettingsToken INIT_BLOCK = invertible("INITIALIZER_BLOCK", StdArrangementTokenType.ENTRY_TYPE);
    
    public static final ArrangementSettingsToken NAMESPACE = invertible("NAMESPACE", StdArrangementTokenType.ENTRY_TYPE);
    
    public static final ArrangementSettingsToken TRAIT = invertible("TRAIT", StdArrangementTokenType.ENTRY_TYPE);

    // xml use only two entry types -> invertible tokens make no sense
    
    public static final ArrangementSettingsToken XML_TAG = token("XML_TAG", "arrangement.settings.text.entry.type.xml.tag", StdArrangementTokenType.ENTRY_TYPE);
    
    public static final StdArrangementSettingsToken XML_ATTRIBUTE = token("XML_ATTRIBUTE", "arrangement.settings.text.entry.type.xml.attribute", StdArrangementTokenType.ENTRY_TYPE);

    private static final Supplier<Set<ArrangementSettingsToken>> TOKENS = collectFields(EntryType.class);

    private EntryType() {
    }

    
    public static Set<ArrangementSettingsToken> values() {
      return TOKENS.get();
    }
  }

  public static class Modifier {
    
    public static final ArrangementSettingsToken PUBLIC = invertible("PUBLIC", StdArrangementTokenType.MODIFIER);
    
    public static final ArrangementSettingsToken PROTECTED = invertible("PROTECTED", StdArrangementTokenType.MODIFIER);
    
    public static final ArrangementSettingsToken PRIVATE = invertible("PRIVATE", StdArrangementTokenType.MODIFIER);
    
    public static final ArrangementSettingsToken PACKAGE_PRIVATE = invertible("PACKAGE_PRIVATE", StdArrangementTokenType.MODIFIER);
    
    public static final ArrangementSettingsToken STATIC = invertible("STATIC", StdArrangementTokenType.MODIFIER);
    
    public static final ArrangementSettingsToken FINAL = invertible("FINAL", StdArrangementTokenType.MODIFIER);
    
    public static final ArrangementSettingsToken TRANSIENT = invertible("TRANSIENT", StdArrangementTokenType.MODIFIER);
    
    public static final ArrangementSettingsToken VOLATILE = invertible("VOLATILE", StdArrangementTokenType.MODIFIER);
    
    public static final ArrangementSettingsToken SYNCHRONIZED = invertible("SYNCHRONIZED", StdArrangementTokenType.MODIFIER);
    
    public static final ArrangementSettingsToken ABSTRACT = invertible("ABSTRACT", StdArrangementTokenType.MODIFIER);
    
    public static final ArrangementSettingsToken OVERRIDE = invertible("OVERRIDE", StdArrangementTokenType.MODIFIER);
    
    public static final ArrangementSettingsToken GETTER = compositeToken("GETTER", StdArrangementTokenType.MODIFIER, METHOD, PUBLIC);
    
    public static final ArrangementSettingsToken SETTER = compositeToken("SETTER", StdArrangementTokenType.MODIFIER, METHOD, PUBLIC);
    
    public static final ArrangementSettingsToken OVERRIDDEN = compositeToken("OVERRIDDEN", StdArrangementTokenType.MODIFIER, METHOD, PUBLIC, PROTECTED);
    private static final Supplier<Set<ArrangementSettingsToken>> TOKENS = collectFields(Modifier.class);

    public static final Set<ArrangementSettingsToken> MODIFIER_AS_TYPE = Set.of(GETTER, SETTER, OVERRIDDEN);

    private Modifier() {
    }

    
    public static Set<ArrangementSettingsToken> values() {
      return TOKENS.get();
    }
  }

  public static class Grouping {
    
    public static final ArrangementSettingsToken GETTERS_AND_SETTERS = token("GETTERS_AND_SETTERS", "arrangement.settings.groups.getters.and.setters.together", StdArrangementTokenType.GROUPING);
    
    public static final ArrangementSettingsToken OVERRIDDEN_METHODS = token("OVERRIDDEN_METHODS", "arrangement.settings.groups.overridden.methods", StdArrangementTokenType.GROUPING);
    
    public static final ArrangementSettingsToken DEPENDENT_METHODS = token("DEPENDENT_METHODS", "arrangement.settings.groups.dependent.methods", StdArrangementTokenType.GROUPING);
    
    public static final ArrangementSettingsToken GROUP_PROPERTY_FIELD_WITH_GETTER_SETTER =
            token("GROUP_PROPERTY_FIELD_WITH_GETTER_SETTER", "arrangement.settings.groups.property.field", StdArrangementTokenType.GROUPING);

    private Grouping() {
    }
  }

  public static class Order {
    
    public static final ArrangementSettingsToken KEEP = token("KEEP", "arrangement.settings.order.type.keep", StdArrangementTokenType.ORDER);
    
    public static final ArrangementSettingsToken BY_NAME = token("BY_NAME", "arrangement.settings.order.type.by.name", StdArrangementTokenType.ORDER);
    
    public static final ArrangementSettingsToken DEPTH_FIRST = token("DEPTH_FIRST", "arrangement.settings.order.type.depth.first", StdArrangementTokenType.ORDER);
    
    public static final ArrangementSettingsToken BREADTH_FIRST = token("BREADTH_FIRST", "arrangement.settings.order.type.breadth.first", StdArrangementTokenType.ORDER);

    private Order() {
    }
  }
}
