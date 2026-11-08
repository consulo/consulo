// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.dataFlow.value;

import consulo.language.dataFlow.types.DfType;
import org.jspecify.annotations.Nullable;

/**
 * Represents a condition to be applied to DFA memory state.
 * This interface has only two implementations: {@link Exact} and {@link DfaRelation}.
 * No other implementations allowed.
 */
public sealed interface DfaCondition permits DfaCondition.Exact, DfaRelation {
  /**
   * @return a condition which is the opposite to this condition
   */
  DfaCondition negate();

  default DfaCondition correctForRelationResult(boolean result) {
    return this;
  }

  /**
   * @return always true condition; singleton object
   */
  static DfaCondition getTrue() {
    return Exact.TRUE;
  }

  /**
   * @return always false condition; singleton object
   */
  static DfaCondition getFalse() {
    return Exact.FALSE;
  }

  /**
   * @return condition with unknown value; singleton object
   */
  static DfaCondition getUnknown() {
    return Exact.UNKNOWN;
  }

  /**
   * @return true if result of this condition cannot be known exactly in any possible memory state
   */
  boolean isUnknown();

  /**
   * @see DfaValue#cond(RelationType, DfaValue)
   */
  static DfaCondition createCondition(DfaValue left, RelationType relationType, DfaValue right) {
    Exact value = Exact.tryEvaluate(left, relationType, right);
    if (value != null) return value;
    DfaRelation relation = DfaRelation.createRelation(left, relationType, right);
    if (relation != null) return relation;
    return Exact.UNKNOWN;
  }

  static @Nullable Exact tryEvaluate(DfType leftType, RelationType relationType, DfType rightType) {
    if (relationType == RelationType.IS || relationType == RelationType.IS_NOT) {
      boolean isSuperState = rightType.isSuperType(leftType);
      if (isSuperState) {
        return Exact.fromBoolean(relationType == RelationType.IS);
      }
      boolean isDistinct = rightType.meet(leftType) == DfType.BOTTOM;
      if (isDistinct) {
        return Exact.fromBoolean(relationType == RelationType.IS_NOT);
      }
    } else {
      DfType meetRelation = leftType.meetRelation(relationType, rightType);
      DfType meetNegatedRelation = leftType.meetRelation(relationType.getNegated(), rightType);
      if (meetRelation == DfType.BOTTOM) {
        // both could be BOTTOM if declared type mismatches
        return meetNegatedRelation == DfType.BOTTOM ? null : Exact.FALSE;
      }
      if (meetNegatedRelation == DfType.BOTTOM) {
        return Exact.TRUE;
      }
    }

    return null;
  }

  enum Exact implements DfaCondition {
    TRUE("TRUE"),
    FALSE("FALSE"),
    UNKNOWN("UNKNOWN");
    
    private final String myName;

    Exact(String name) {
      myName = name;
    }

    @Override
    public String toString() {
      return myName;
    }


    @Override
    public DfaCondition negate() {
      if (this == TRUE) return FALSE;
      if (this == FALSE) return TRUE;
      return UNKNOWN;
    }

    @Override
    public boolean isUnknown() {
      return this == UNKNOWN;
    }

    private static Exact fromBoolean(boolean value) {
      return value ? TRUE : FALSE;
    }

    private static @Nullable Exact tryEvaluate(DfaValue dfaLeft, RelationType relationType, DfaValue dfaRight) {
      DfaValue sentinel = dfaLeft.getFactory().getSentinel();
      if ((dfaLeft == sentinel) || (dfaRight == sentinel)) {
        return fromBoolean((dfaLeft == sentinel && dfaRight == sentinel) == (relationType == RelationType.EQ));
      }
      if (dfaLeft == dfaRight && dfaLeft instanceof DfaBinOpValue) {
        return fromBoolean(relationType.isSubRelation(RelationType.EQ));
      }
      DfType leftType = dfaLeft.getDfType();
      DfType rightType = dfaRight.getDfType();

      return DfaCondition.tryEvaluate(leftType, relationType, rightType);
    }
  }
}
