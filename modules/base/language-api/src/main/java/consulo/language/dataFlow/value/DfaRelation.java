// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package consulo.language.dataFlow.value;


/**
 * A condition that represents a relation between two DfaValues
 */
public record DfaRelation(DfaValue leftOperand, DfaValue rightOperand, RelationType relationType) implements DfaCondition {
  @Override
  public DfaRelation negate() {
    return new DfaRelation(leftOperand, rightOperand, relationType.getNegated());
  }

  @Override
  public DfaCondition correctForRelationResult(boolean result) {
    DfaValue newLeft = leftOperand;
    DfaValue newRight = rightOperand;
    if (newLeft instanceof DfaTypeValue leftTypeValue) {
      newLeft = newLeft.getFactory().fromDfType(leftTypeValue.getDfType().correctForRelationResult(relationType, result));
    }
    if (newRight instanceof DfaTypeValue rightTypeValue) {
      newRight = newRight.getFactory().fromDfType(rightTypeValue.getDfType().correctForRelationResult(relationType, result));
    }
    return newLeft == leftOperand && newRight == rightOperand ? this :
           DfaCondition.createCondition(newLeft, relationType, newRight);
  }
  
  @Override
  public boolean isUnknown() {
    return leftOperand instanceof DfaTypeValue && rightOperand instanceof DfaTypeValue;
  }

  public static DfaRelation createRelation(DfaValue dfaLeft, RelationType relationType, DfaValue dfaRight) {
    if (dfaLeft instanceof DfaVariableValue || dfaLeft instanceof DfaWrappedValue || dfaLeft instanceof DfaBinOpValue
        || dfaRight instanceof DfaVariableValue || dfaRight instanceof DfaWrappedValue || dfaRight instanceof DfaBinOpValue) {
      if (!(dfaLeft instanceof DfaVariableValue || dfaLeft instanceof DfaWrappedValue || dfaLeft instanceof DfaBinOpValue) ||
          (dfaRight instanceof DfaBinOpValue && !(dfaLeft instanceof DfaBinOpValue))) {
        RelationType flipped = relationType.getFlipped();
        return flipped == null ? null : new DfaRelation(dfaRight, dfaLeft, flipped);
      }
    }
    return new DfaRelation(dfaLeft, dfaRight, relationType);
  }

  public boolean isEquality() {
    return relationType == RelationType.EQ;
  }

  @Override
  public String toString() {
    return leftOperand + " " + relationType + " " + rightOperand;
  }
}
