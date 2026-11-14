// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.editor.refactoring.suggested;

import consulo.language.editor.refactoring.suggested.SuggestedRefactoringExecution.NewParameterValue;
import consulo.language.editor.refactoring.suggested.SuggestedRefactoringSupport.Signature;
import consulo.language.psi.PsiCodeFragment;
import consulo.localize.LocalizeValue;
import consulo.ui.ex.awt.ValidationInfo;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.util.List;
import java.util.Objects;

/**
 * A service providing information required for building user-interface of Change Signature refactoring.
 */
public abstract class SuggestedRefactoringUI {
    /**
     * Creates an instance of helper {@link SignaturePresentationBuilder} class which is used to build {@link SignatureChangePresentationModel}.
     *
     * @param signature      signature (either old or new) to build presentation for
     * @param otherSignature other signature (either new or old)
     * @param isOldSignature true if {@code signature} represents the old signature, or false otherwise
     */
    public abstract SignaturePresentationBuilder createSignaturePresentationBuilder(
        Signature signature,
        Signature otherSignature,
        boolean isOldSignature
    );

    public SignatureChangePresentationModel buildSignatureChangePresentation(Signature oldSignature, Signature newSignature) {
        SignaturePresentationBuilder oldBuilder = createSignaturePresentationBuilder(oldSignature, newSignature, true);
        oldBuilder.buildPresentation();
        List<SignatureChangePresentationModel.TextFragment> oldSignaturePresentation = oldBuilder.getResult();

        SignaturePresentationBuilder newBuilder = createSignaturePresentationBuilder(newSignature, oldSignature, false);
        newBuilder.buildPresentation();
        List<SignatureChangePresentationModel.TextFragment> newSignaturePresentation = newBuilder.getResult();

        SignatureChangePresentationModel model = new SignatureChangePresentationModel(oldSignaturePresentation, newSignaturePresentation);

        return model.improvePresentation();
    }

    public static final class NewParameterData {
        private final String myPresentableName;
        private final PsiCodeFragment myValueFragment;
        private final boolean myOfferToUseAnyVariable;
        private final LocalizeValue myPlaceholderText;
        private final @Nullable NewParameterAdditionalData myAdditionalData;
        private final boolean mySuggestRename;

        public NewParameterData(String presentableName, PsiCodeFragment valueFragment, boolean offerToUseAnyVariable) {
            this(presentableName, valueFragment, offerToUseAnyVariable, LocalizeValue.empty(), null, false);
        }

        public NewParameterData(
            String presentableName,
            PsiCodeFragment valueFragment,
            boolean offerToUseAnyVariable,
            LocalizeValue placeholderText,
            @Nullable NewParameterAdditionalData additionalData,
            boolean suggestRename
        ) {
            myPresentableName = presentableName;
            myValueFragment = valueFragment;
            myOfferToUseAnyVariable = offerToUseAnyVariable;
            myPlaceholderText = placeholderText;
            myAdditionalData = additionalData;
            mySuggestRename = suggestRename;
        }

        public String getPresentableName() {
            return myPresentableName;
        }

        public PsiCodeFragment getValueFragment() {
            return myValueFragment;
        }

        public boolean getOfferToUseAnyVariable() {
            return myOfferToUseAnyVariable;
        }

        public LocalizeValue getPlaceholderText() {
            return myPlaceholderText;
        }

        public @Nullable NewParameterAdditionalData getAdditionalData() {
            return myAdditionalData;
        }

        public boolean getSuggestRename() {
            return mySuggestRename;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof NewParameterData that &&
                myOfferToUseAnyVariable == that.myOfferToUseAnyVariable &&
                mySuggestRename == that.mySuggestRename &&
                myPresentableName.equals(that.myPresentableName) &&
                myValueFragment.equals(that.myValueFragment) &&
                myPlaceholderText.equals(that.myPlaceholderText) &&
                Objects.equals(myAdditionalData, that.myAdditionalData);
        }

        @Override
        public int hashCode() {
            return Objects.hash(myPresentableName, myValueFragment, myOfferToUseAnyVariable, myPlaceholderText, myAdditionalData, mySuggestRename);
        }
    }

    /**
     * Language-specific information to be stored in {@link NewParameterData}.
     * <p>
     * Don't put any PSI-related objects here.
     */
    public interface NewParameterAdditionalData {
        @Override
        boolean equals(Object other);
    }

    /**
     * Extracts data about new parameters to offer the user to specify its values for updating calls.
     */
    public abstract List<NewParameterData> extractNewParameterData(SuggestedChangeSignatureData data);

    /**
     * Validates value for {@code data} typed in {@code component}.
     * This method should be very fast since it is called on any change for any parameter even if the updated parameter is not {@code data}.
     */
    public @Nullable ValidationInfo validateValue(NewParameterData data, @Nullable JComponent component) {
        return null;
    }

    /**
     * Extracts value for a new parameter from code fragment after its editing by the user.
     *
     * @return entered expression or <i>null</i> if no expression in the code fragment
     */
    public abstract NewParameterValue.@Nullable Expression extractValue(PsiCodeFragment fragment);

    /**
     * Use this implementation of {@link SuggestedRefactoringUI}, if only Rename refactoring is supported for the language.
     */
    public static final class RenameOnly extends SuggestedRefactoringUI {
        public static final RenameOnly INSTANCE = new RenameOnly();

        private RenameOnly() {
        }

        @Override
        public SignaturePresentationBuilder createSignaturePresentationBuilder(
            Signature signature,
            Signature otherSignature,
            boolean isOldSignature
        ) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<NewParameterData> extractNewParameterData(SuggestedChangeSignatureData data) {
            throw new UnsupportedOperationException();
        }

        @Override
        public NewParameterValue.@Nullable Expression extractValue(PsiCodeFragment fragment) {
            throw new UnsupportedOperationException();
        }
    }
}
