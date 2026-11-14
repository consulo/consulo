// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.editor.refactoring.suggested;

import consulo.language.editor.refactoring.suggested.SignatureChangePresentationModel.Effect;
import consulo.language.editor.refactoring.suggested.SignatureChangePresentationModel.TextFragment;
import consulo.language.editor.refactoring.suggested.SignatureChangePresentationModel.TextFragment.Group;
import consulo.language.editor.refactoring.suggested.SignatureChangePresentationModel.TextFragment.Leaf;
import consulo.language.editor.refactoring.suggested.SignatureChangePresentationModel.TextFragment.LineBreak;
import consulo.language.editor.refactoring.suggested.SuggestedRefactoringSupport.Parameter;
import consulo.language.editor.refactoring.suggested.SuggestedRefactoringSupport.Signature;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper class to build presentation for one signature (either old or new).
 * <p>
 * Note that presentation provided by this class is not the final presentation displayed to the user.
 * {@link SignatureChangePresentationModel#improvePresentation} is used to simplify its appearance:
 * <ul>
 * <li>{@link Effect#Moved} should not be used, it's generated automatically based on connections between fragments
 * (see {@link TextFragment#getConnectionId()})</li>
 * <li>Only connections necessary to display moved elements are shown</li>
 * </ul>
 */
public abstract class SignaturePresentationBuilder {
    @FunctionalInterface
    public interface ParameterFragmentsBuilder {
        void build(List<TextFragment> fragments, Parameter parameter, @Nullable Parameter correspondingParameter);
    }

    protected final Signature signature;
    protected final Signature otherSignature;
    protected final boolean isOldSignature;

    protected final List<TextFragment> fragments = new ArrayList<>();

    /**
     * @param signature      signature (either old or new) to build presentation for
     * @param otherSignature other signature (either new or old)
     * @param isOldSignature true if {@code signature} represents the old signature, or false otherwise
     */
    protected SignaturePresentationBuilder(Signature signature, Signature otherSignature, boolean isOldSignature) {
        this.signature = signature;
        this.otherSignature = otherSignature;
        this.isOldSignature = isOldSignature;
    }

    public List<TextFragment> getResult() {
        return fragments;
    }

    public abstract void buildPresentation();

    protected Effect effect(String value, @Nullable String otherValue) {
        if (otherValue == null || otherValue.isEmpty()) {
            return isOldSignature ? Effect.Removed : Effect.Added;
        }
        else {
            return !otherValue.equals(value) ? Effect.Modified : Effect.None;
        }
    }

    protected Leaf leaf(String value, @Nullable String otherValue) {
        Effect effect = effect(value, otherValue);
        return new Leaf(value, effect);
    }

    protected void buildParameterList(ParameterFragmentsBuilder parameterBuilder) {
        buildParameterList("(", ")", parameterBuilder);
    }

    protected void buildParameterList(String prefix, String suffix, ParameterFragmentsBuilder parameterBuilder) {
        buildParameterList(prefix, suffix, signature.getParameters(), parameterBuilder);
    }

    protected void buildParameterList(String prefix, String suffix, List<Parameter> parameters, ParameterFragmentsBuilder parameterBuilder) {
        fragments.add(new Leaf(prefix));
        if (!parameters.isEmpty()) {
            fragments.add(new LineBreak("", true));
        }

        for (int index = 0; index < parameters.size(); index++) {
            Parameter parameter = parameters.get(index);
            if (index > 0) {
                fragments.add(new Leaf(","));
                fragments.add(new LineBreak(" ", true));
            }

            Parameter correspondingParameter = otherSignature.parameterById(parameter.getId());
            Object connectionId = correspondingParameter != null ? correspondingParameter.getId() : null;

            Effect effect;
            if (isOldSignature) {
                effect = correspondingParameter == null ? Effect.Removed : Effect.None;
            }
            else {
                effect = correspondingParameter == null ? Effect.Added : Effect.None;
            }

            List<TextFragment> children = new ArrayList<>();
            parameterBuilder.build(children, parameter, correspondingParameter);
            fragments.add(new Group(children, effect, connectionId));
        }

        fragments.add(new LineBreak("", false));
        fragments.add(new Leaf(suffix));
    }
}
