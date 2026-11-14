// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor.refactoring.suggested;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.document.util.TextRange;
import consulo.language.Language;
import consulo.language.extension.ByLanguageValue;
import consulo.language.extension.LanguageExtension;
import consulo.language.extension.LanguageOneToOne;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.util.PsiTreeUtil;
import org.jspecify.annotations.Nullable;

import java.util.*;

/**
 * Language extension to implement to support suggested Rename and/or Change Signature refactorings.
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface SuggestedRefactoringSupport extends LanguageExtension {
    ExtensionPointCacheKey<SuggestedRefactoringSupport, ByLanguageValue<SuggestedRefactoringSupport>> KEY =
        ExtensionPointCacheKey.create("SuggestedRefactoringSupport", LanguageOneToOne.build());

    static @Nullable SuggestedRefactoringSupport forLanguage(Language language) {
        return Application.get().getExtensionPoint(SuggestedRefactoringSupport.class).getOrBuildCache(KEY).get(language);
    }

    /**
     * Returns true, if given PsiElement is an anchor element where suggested refactoring can start.
     * <p>
     * It could be either a declaration or a call-site if suggested refactoring from the call-site is supported.
     * <p>
     * Note, that if Change Signature is supported then individual parameters must not be considered as anchors
     * for they are part of a bigger anchor (method or function).
     */
    @RequiredReadAction
    boolean isAnchor(PsiElement psiElement);

    /**
     * Returns "signature range" for a given anchor.
     * <p>
     * Signature range is a range that contains all properties taken into account by Change Signature refactoring.
     * If only Rename refactoring is supported for the given declaration, the name identifier range must be returned.
     * <p>
     * Only PsiElement's that are classified as anchor by {@link #isAnchor} method must be passed to this method.
     *
     * @return signature range for the anchor, or <i>null</i> if anchor is considered unsuitable for refactoring
     * (usually when it has syntax error).
     */
    @RequiredReadAction
    @Nullable
    TextRange signatureRange(PsiElement anchor);

    /**
     * Returns range in the given file taken by imports (if supported by the language).
     * <p>
     * If no imports exist yet in the given file, it must return an empty range within whitespace where the imports are to be inserted.
     *
     * @return range taken by imports, or <i>null</i> if imports are not supported by the language.
     */
    @RequiredReadAction
    @Nullable
    TextRange importsRange(PsiFile psiFile);

    /**
     * Returns name range for a given anchor.
     * <p>
     * Only PsiElement's that are classified as anchors by {@link #isAnchor} method must be passed to this method.
     *
     * @return name range for the anchor, or <i>null</i> if anchor does not have a name.
     */
    @RequiredReadAction
    @Nullable
    TextRange nameRange(PsiElement anchor);

    /**
     * Returns true if there's a syntax error within given anchor that prevents
     * suggested refactoring from successful completion
     */
    @RequiredReadAction
    default boolean hasSyntaxError(PsiElement anchor) {
        TextRange signatureRange = signatureRange(anchor);
        if (signatureRange == null) {
            return true;
        }
        return PsiTreeUtil.hasErrorElementInRange(anchor.getContainingFile(), signatureRange);
    }

    /**
     * Determines if the character can start an identifier in the language.
     * <p>
     * This method is used for suppression of refactoring for a declaration which has been just typed by the user.
     */
    boolean isIdentifierStart(char c);

    /**
     * Determines if an identifier can contain the character.
     * <p>
     * This method is used for suppression of refactoring for a declaration which has been just typed by the user.
     */
    boolean isIdentifierPart(char c);

    /**
     * A service transforming a sequence of declaration states into {@link SuggestedRefactoringState}.
     * <p>
     * Use {@link SuggestedRefactoringStateChanges.RenameOnly} if only Rename refactoring is supported.
     */
    SuggestedRefactoringStateChanges getStateChanges();

    /**
     * A service determining available refactoring for a given {@link SuggestedRefactoringState}.
     * <p>
     * Use {@link SuggestedRefactoringAvailability.RenameOnly} if only Rename refactoring is supported.
     */
    SuggestedRefactoringAvailability getAvailability();

    /**
     * A service providing information required for building user-interface of Change Signature refactoring.
     * <p>
     * Use {@link SuggestedRefactoringUI.RenameOnly} if only Rename refactoring is supported.
     */
    SuggestedRefactoringUI getUi();

    /**
     * A service performing actual changes in the code when user executes suggested refactoring.
     * <p>
     * Use {@link SuggestedRefactoringExecution.RenameOnly} if only Rename refactoring is supported.
     */
    SuggestedRefactoringExecution getExecution();

    @RequiredReadAction
    default @Nullable PsiElement anchorByOffset(PsiFile psiFile, int offset) {
        PsiElement element = psiFile.findElementAt(offset);
        while (element != null) {
            if (isAnchor(element)) {
                return element;
            }
            if (element instanceof PsiFile) {
                break;
            }
            element = element.getParent();
        }
        return null;
    }

    /**
     * A class with data representing declaration signature.
     * <p>
     * This data is used mainly for signature change presentation and for detection of refactoring availability.
     * No PSI-related objects must be referenced by instances of this class and all PSI-related information
     * that is required to perform the refactoring must be extracted from the declaration itself prior to perform the refactoring.
     */
    final class Signature {
        private final String myName;
        private final @Nullable String myType;
        private final List<Parameter> myParameters;
        private final @Nullable SignatureAdditionalData myAdditionalData;
        private final Map<String, Parameter> myNameToParameter;
        private final Map<Object, Parameter> myIdToParameter = new HashMap<>();
        private final Map<Parameter, Integer> myParameterToIndex = new HashMap<>();

        private Signature(
            String name,
            @Nullable String type,
            List<Parameter> parameters,
            @Nullable SignatureAdditionalData additionalData,
            Map<String, Parameter> nameToParameter
        ) {
            myName = name;
            myType = type;
            myParameters = parameters;
            myAdditionalData = additionalData;
            myNameToParameter = nameToParameter;

            for (Parameter parameter : parameters) {
                Parameter prev = myIdToParameter.put(parameter.getId(), parameter);
                if (prev != null) {
                    throw new IllegalArgumentException("Duplicate parameter id: " + parameter.getId());
                }
            }

            for (int index = 0; index < parameters.size(); index++) {
                myParameterToIndex.put(parameters.get(index), index);
            }
        }

        public String getName() {
            return myName;
        }

        public @Nullable String getType() {
            return myType;
        }

        public List<Parameter> getParameters() {
            return myParameters;
        }

        public @Nullable SignatureAdditionalData getAdditionalData() {
            return myAdditionalData;
        }

        public @Nullable Parameter parameterById(Object id) {
            return myIdToParameter.get(id);
        }

        public @Nullable Parameter parameterByName(String name) {
            return myNameToParameter.get(name);
        }

        public int parameterIndex(Parameter parameter) {
            return Objects.requireNonNull(myParameterToIndex.get(parameter));
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Signature signature &&
                myName.equals(signature.myName) &&
                Objects.equals(myType, signature.myType) &&
                Objects.equals(myAdditionalData, signature.myAdditionalData) &&
                myParameters.equals(signature.myParameters);
        }

        @Override
        public int hashCode() {
            return 0;
        }

        /**
         * Factory method, used to create instances of {@link Signature} class.
         *
         * @return create instance of {@link Signature}, or <i>null</i> if it cannot be created due to duplicated parameter names
         */
        public static @Nullable Signature create(
            String name,
            @Nullable String type,
            List<Parameter> parameters,
            @Nullable SignatureAdditionalData additionalData
        ) {
            Map<String, Parameter> nameToParameter = new HashMap<>();
            for (Parameter parameter : parameters) {
                String key = parameter.getName();
                if (nameToParameter.containsKey(key)) {
                    return null;
                }
                nameToParameter.put(key, parameter);
            }
            return new Signature(name, type, List.copyOf(parameters), additionalData, nameToParameter);
        }
    }

    /**
     * A class representing a parameter in {@link Signature}.
     * <p>
     * Parameters with the same {@link #getId() id} represent the same parameter in the old and new signatures.
     * All parameters in the same {@link Signature} must have unique {@link #getId() id}.
     */
    final class Parameter {
        private final Object myId;
        private final String myName;
        private final String myType;
        private final @Nullable ParameterAdditionalData myAdditionalData;

        public Parameter(Object id, String name, String type) {
            this(id, name, type, null);
        }

        public Parameter(Object id, String name, String type, @Nullable ParameterAdditionalData additionalData) {
            myId = id;
            myName = name;
            myType = type;
            myAdditionalData = additionalData;
        }

        public Object getId() {
            return myId;
        }

        public String getName() {
            return myName;
        }

        public String getType() {
            return myType;
        }

        public @Nullable ParameterAdditionalData getAdditionalData() {
            return myAdditionalData;
        }

        public Parameter withId(Object id) {
            return new Parameter(id, myName, myType, myAdditionalData);
        }

        public Parameter withName(String name) {
            return new Parameter(myId, name, myType, myAdditionalData);
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Parameter parameter &&
                myId.equals(parameter.myId) &&
                myName.equals(parameter.myName) &&
                myType.equals(parameter.myType) &&
                Objects.equals(myAdditionalData, parameter.myAdditionalData);
        }

        @Override
        public int hashCode() {
            return Objects.hash(myId, myName, myType, myAdditionalData);
        }

        @Override
        public String toString() {
            return "Parameter(id=" + myId + ", name=" + myName + ", type=" + myType + ", additionalData=" + myAdditionalData + ")";
        }
    }

    /**
     * Language-specific information to be stored in {@link Signature}.
     * <p>
     * Don't put any PSI-related objects here.
     */
    interface SignatureAdditionalData {
        @Override
        boolean equals(Object other);
    }

    /**
     * Language-specific information to be stored in {@link Parameter}.
     * <p>
     * Don't put any PSI-related objects here.
     */
    interface ParameterAdditionalData {
        @Override
        boolean equals(Object other);
    }
}
