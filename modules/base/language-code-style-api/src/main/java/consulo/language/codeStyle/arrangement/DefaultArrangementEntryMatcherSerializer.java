/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.language.codeStyle.arrangement;

import consulo.language.codeStyle.arrangement.match.ArrangementEntryMatcher;
import consulo.language.codeStyle.arrangement.match.StdArrangementEntryMatcher;
import consulo.language.codeStyle.arrangement.model.ArrangementAtomMatchCondition;
import consulo.language.codeStyle.arrangement.model.ArrangementCompositeMatchCondition;
import consulo.language.codeStyle.arrangement.model.ArrangementMatchCondition;
import consulo.language.codeStyle.arrangement.model.ArrangementMatchConditionVisitor;
import consulo.language.codeStyle.arrangement.std.ArrangementSettingsToken;
import consulo.language.codeStyle.arrangement.std.StdArrangementTokenType;
import consulo.language.codeStyle.arrangement.std.StdArrangementTokens;
import consulo.logging.Logger;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.StringUtil;
import org.jdom.Element;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * @author Denis Zhdanov
 * @since 7/19/12 1:00 PM
 */
public class DefaultArrangementEntryMatcherSerializer {

  private static final Comparator<ArrangementMatchCondition> CONDITION_COMPARATOR = new Comparator<ArrangementMatchCondition>() {

    @Override
    public int compare(ArrangementMatchCondition c1, ArrangementMatchCondition c2) {
      boolean isAtom1 = c1 instanceof ArrangementAtomMatchCondition;
      boolean isAtom2 = c2 instanceof ArrangementAtomMatchCondition;
      if (isAtom1 ^ isAtom2) {
        return isAtom1 ? 1 : -1; // Composite conditions before atom conditions.
      }
      else if (!isAtom1) {
        return 0;
      }

      ArrangementAtomMatchCondition atom1 = (ArrangementAtomMatchCondition)c1;
      ArrangementAtomMatchCondition atom2 = (ArrangementAtomMatchCondition)c2;
      int cmp = atom1.getType().compareTo(atom2.getType());
      if (cmp == 0) {
        cmp = atom1.getValue().toString().compareTo(atom2.getValue().toString());
      }
      return cmp;
    }
  };

  @Nonnull
  private static final Logger LOG = Logger.getInstance(DefaultArrangementEntryMatcherSerializer.class);

  @Nonnull
  private static final String COMPOSITE_CONDITION_NAME = "AND";

  private final DefaultArrangementSettingsSerializer.Mixin myMixin;

  public DefaultArrangementEntryMatcherSerializer(DefaultArrangementSettingsSerializer.Mixin mixin) {
    myMixin = mixin;
  }

  @SuppressWarnings("MethodMayBeStatic")
  @Nullable
  public <T extends ArrangementEntryMatcher> Element serialize(@Nonnull T matcher) {
    if (matcher instanceof StdArrangementEntryMatcher) {
      return serialize(((StdArrangementEntryMatcher)matcher).getCondition());
    }
    LOG.warn(String.format(
            "Can't serialize arrangement entry matcher of class '%s'. Reason: expected to find '%s' instance instead",
            matcher.getClass(), StdArrangementEntryMatcher.class
    ));
    return null;
  }

  @Nonnull
  private static Element serialize(@Nonnull ArrangementMatchCondition condition) {
    MySerializationVisitor visitor = new MySerializationVisitor();
    condition.invite(visitor);
    return visitor.result;
  }

  @SuppressWarnings("MethodMayBeStatic")
  @Nullable
  public StdArrangementEntryMatcher deserialize(@Nonnull Element matcherElement) {
    ArrangementMatchCondition condition = deserializeCondition(matcherElement);
    return condition == null ? null : new StdArrangementEntryMatcher(condition);
  }

  @Nullable
  private ArrangementMatchCondition deserializeCondition(@Nonnull Element matcherElement) {
    String name = matcherElement.getName();
    if (COMPOSITE_CONDITION_NAME.equals(name)) {
      ArrangementCompositeMatchCondition composite = new ArrangementCompositeMatchCondition();
      for (Object child : matcherElement.getChildren()) {
        ArrangementMatchCondition deserialised = deserializeCondition((Element)child);
        if (deserialised != null) {
          composite.addOperand(deserialised);
        }
      }
      return composite;
    }
    else {
      return deserializeAtomCondition(matcherElement);
    }
  }

  @Nullable
  private ArrangementMatchCondition deserializeAtomCondition(@Nonnull Element matcherElement) {
    String id = matcherElement.getName();
    ArrangementSettingsToken token = StdArrangementTokens.byId(id);
    boolean processInnerText = true;

    if (token != null
        && (StdArrangementTokens.General.TYPE.equals(token) || StdArrangementTokens.General.MODIFIER.equals(token)))
    {
      // Backward compatibility with old arrangement settings format.
      id = matcherElement.getText();
      if (StringUtil.isEmpty(id)) {
        LOG.warn("Can't deserialize match condition at legacy format");
        return null;
      }
      token = StdArrangementTokens.byId(id);
      processInnerText = false;
    }

    if (token == null) {
      token = myMixin.deserializeToken(id);
    }
    if (token == null) {
      LOG.warn(String.format("Can't deserialize match condition with id '%s'", id));
      return null;
    }

    Object value = token;
    String text = matcherElement.getText();
    if (text != null && processInnerText) {
      text = StringUtil.unescapeStringCharacters(matcherElement.getText());
      if (!StringUtil.isEmpty(text)) {
        value = text;
      }
    }
    return new ArrangementAtomMatchCondition(token, value);
  }

  private static class MySerializationVisitor implements ArrangementMatchConditionVisitor {

    Element result;
    Element parent;

    @Override
    public void visit(@Nonnull ArrangementAtomMatchCondition condition) {
      ArrangementSettingsToken type = condition.getType();
      Element element = new Element(type.getId());
      if (StdArrangementTokenType.REG_EXP.is(type)) {
        element.setText(StringUtil.escapeStringCharacters(condition.getValue().toString()));
      }
      register(element);
    }

    @Override
    public void visit(@Nonnull ArrangementCompositeMatchCondition condition) {
      Element composite = new Element(COMPOSITE_CONDITION_NAME);
      register(composite);
      parent = composite;
      List<ArrangementMatchCondition> operands = new ArrayList<>(condition.getOperands());
      ContainerUtil.sort(operands, CONDITION_COMPARATOR);
      for (ArrangementMatchCondition c : operands) {
        c.invite(this);
      }
    }

    private void register(@Nonnull Element element) {
      if (result == null) {
        result = element;
      }
      if (parent != null) {
        parent.addContent(element);
      }
    }
  }
}
