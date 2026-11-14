// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.editor.refactoring.suggested;

import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public final class SignatureChangePresentationModel {
    private final List<TextFragment> myOldSignature;
    private final List<TextFragment> myNewSignature;

    public SignatureChangePresentationModel(List<TextFragment> oldSignature, List<TextFragment> newSignature) {
        myOldSignature = oldSignature;
        myNewSignature = newSignature;
    }

    public List<TextFragment> getOldSignature() {
        return myOldSignature;
    }

    public List<TextFragment> getNewSignature() {
        return myNewSignature;
    }

    public static abstract sealed class TextFragment permits TextFragment.Leaf, TextFragment.Group, TextFragment.LineBreak {
        private final @Nullable Object myConnectionId;
        private final Effect myEffect;

        protected TextFragment(@Nullable Object connectionId, Effect effect) {
            myConnectionId = connectionId;
            myEffect = effect;
        }

        public @Nullable Object getConnectionId() {
            return myConnectionId;
        }

        public Effect getEffect() {
            return myEffect;
        }

        public abstract void forSelfAndDescendants(Consumer<TextFragment> action);

        public abstract boolean anySelfOrDescendants(Predicate<TextFragment> predicate);

        public abstract TextFragment withConnectionId(@Nullable Object connectionId);

        public abstract TextFragment withEffect(Effect effect);

        public static final class Leaf extends TextFragment {
            private final String myText;

            public Leaf(String text) {
                this(text, Effect.None, null);
            }

            public Leaf(String text, Effect effect) {
                this(text, effect, null);
            }

            public Leaf(String text, Effect effect, @Nullable Object connectionId) {
                super(connectionId, effect);
                myText = text;
            }

            public String getText() {
                return myText;
            }

            @Override
            public void forSelfAndDescendants(Consumer<TextFragment> action) {
                action.accept(this);
            }

            @Override
            public boolean anySelfOrDescendants(Predicate<TextFragment> predicate) {
                return predicate.test(this);
            }

            @Override
            public Leaf withConnectionId(@Nullable Object connectionId) {
                return new Leaf(myText, getEffect(), connectionId);
            }

            @Override
            public Leaf withEffect(Effect effect) {
                return new Leaf(myText, effect, getConnectionId());
            }
        }

        public static final class Group extends TextFragment {
            private final List<TextFragment> myChildren;

            public Group(List<TextFragment> children, Effect effect, @Nullable Object connectionId) {
                super(connectionId, effect);
                myChildren = children;
            }

            public List<TextFragment> getChildren() {
                return myChildren;
            }

            @Override
            public void forSelfAndDescendants(Consumer<TextFragment> action) {
                action.accept(this);
                forAllFragments(myChildren, action);
            }

            @Override
            public boolean anySelfOrDescendants(Predicate<TextFragment> predicate) {
                if (predicate.test(this)) {
                    return true;
                }
                for (TextFragment child : myChildren) {
                    if (child.anySelfOrDescendants(predicate)) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            public Group withConnectionId(@Nullable Object connectionId) {
                return new Group(myChildren, getEffect(), connectionId);
            }

            @Override
            public Group withEffect(Effect effect) {
                return new Group(myChildren, effect, getConnectionId());
            }
        }

        public static final class LineBreak extends TextFragment {
            private final String mySpaceInHorizontalMode;
            private final boolean myIndentAfter;

            public LineBreak(String spaceInHorizontalMode, boolean indentAfter) {
                super(null, Effect.None);
                mySpaceInHorizontalMode = spaceInHorizontalMode;
                myIndentAfter = indentAfter;
            }

            public String getSpaceInHorizontalMode() {
                return mySpaceInHorizontalMode;
            }

            public boolean getIndentAfter() {
                return myIndentAfter;
            }

            @Override
            public void forSelfAndDescendants(Consumer<TextFragment> action) {
                action.accept(this);
            }

            @Override
            public boolean anySelfOrDescendants(Predicate<TextFragment> predicate) {
                return predicate.test(this);
            }

            @Override
            public TextFragment withConnectionId(@Nullable Object connectionId) {
                throw new UnsupportedOperationException();
            }

            @Override
            public TextFragment withEffect(Effect effect) {
                throw new UnsupportedOperationException();
            }
        }
    }

    public enum Effect {
        None, Modified, Added, Removed, Moved
    }

    public static void forAllFragments(List<TextFragment> fragments, Consumer<TextFragment> action) {
        for (TextFragment fragment : fragments) {
            fragment.forSelfAndDescendants(action);
        }
    }

    public void checkCorrectness() {
        checkConnectionsAtTopLevelOnly(myOldSignature);
        checkConnectionsAtTopLevelOnly(myNewSignature);

        Set<Object> idSet1 = connectionIdSet(myOldSignature);
        Set<Object> idSet2 = connectionIdSet(myNewSignature);
        if (!idSet1.equals(idSet2)) {
            throw new IllegalStateException("Mismatching set of connectionId's for oldSignature and newSignature");
        }

        myOldSignature.forEach(SignatureChangePresentationModel::checkEffectWithConnection);
        myNewSignature.forEach(SignatureChangePresentationModel::checkEffectWithConnection);
    }

    private static void checkConnectionsAtTopLevelOnly(List<TextFragment> fragments) {
        for (TextFragment fragment : fragments) {
            fragment.forSelfAndDescendants(it -> {
                if (it != fragment && it.getConnectionId() != null) {
                    throw new IllegalStateException("Connections supported only for top-level fragments");
                }
            });
        }
    }

    private static Set<Object> connectionIdSet(List<TextFragment> fragments) {
        Set<Object> set = new HashSet<>();
        for (TextFragment fragment : fragments) {
            Object id = fragment.getConnectionId();
            if (id == null) {
                continue;
            }
            boolean added = set.add(id);
            if (!added) {
                throw new IllegalStateException("Duplicate connectionId: " + id);
            }
        }
        return set;
    }

    private static void checkEffectWithConnection(TextFragment fragment) {
        if (fragment.getConnectionId() != null) {
            Effect effect = fragment.getEffect();
            if (effect != Effect.None && effect != Effect.Modified && effect != Effect.Moved) {
                throw new IllegalStateException("Incorrect effect for fragment with connection: " + effect);
            }
        }
    }

    public SignatureChangePresentationModel improvePresentation() {
        SignatureChangePresentationModel model = this;

        model.checkCorrectness();

        Consumer<TextFragment> checkNoMovedEffect = it -> {
            if (it.getEffect() == Effect.Moved) {
                throw new IllegalArgumentException("Don't use Effect.Moved, it's generated automatically by improvePresentation()");
            }
        };

        forAllFragments(model.myOldSignature, checkNoMovedEffect);
        forAllFragments(model.myNewSignature, checkNoMovedEffect);

        model = model.dropRedundantConnections();
        model = model.addMovedEffectForAllConnections();
        model = model.dropObviousSingleConnection();

        model.checkCorrectness();

        return model;
    }

    private SignatureChangePresentationModel dropRedundantConnections() {
        Set<Object> connectionIdsWithModifications = new HashSet<>();
        for (TextFragment fragment : myOldSignature) {
            if (fragment.getConnectionId() != null && hasModifications(fragment)) {
                connectionIdsWithModifications.add(fragment.getConnectionId());
            }
        }
        for (TextFragment fragment : myNewSignature) {
            if (fragment.getConnectionId() != null && hasModifications(fragment)) {
                connectionIdsWithModifications.add(fragment.getConnectionId());
            }
        }

        // Let's find the longest subsequence of elements in the new signature connected to elements in the old signature
        // with increasing indices. We will display elements in this subsequence as not moved.

        Map<Object, Integer> idToOldIndex = new HashMap<>();
        for (int index = 0; index < myOldSignature.size(); index++) {
            Object connectionId = myOldSignature.get(index).getConnectionId();
            if (connectionId != null) {
                idToOldIndex.put(connectionId, index);
            }
        }

        List<Object> newConnectionIds = new ArrayList<>();
        for (TextFragment fragment : myNewSignature) {
            if (fragment.getConnectionId() != null) {
                newConnectionIds.add(fragment.getConnectionId());
            }
        }

        // we use value function to prefer fragments without modifications to be included in the subsequence
        // (it's better to show element as moved if it has other modifications inside)
        double normalWeight = 1.0;
        double modifiedWeight = 1 - 1.0 / (newConnectionIds.size() + 1); // less than 1.0 but not enough to prefer less elements in the subsequence

        List<Integer> oldIndices = new ArrayList<>(newConnectionIds.size());
        for (Object id : newConnectionIds) {
            oldIndices.add(Objects.requireNonNull(idToOldIndex.get(id)));
        }

        int[] subsequence = MaximumWeightIncreasingSubsequence.findMaximumWeightIncreasingSubsequence(oldIndices, oldIndex -> {
            Object connectionId = Objects.requireNonNull(myOldSignature.get(oldIndex).getConnectionId());
            return connectionIdsWithModifications.contains(connectionId) ? modifiedWeight : normalWeight;
        });
        Set<Object> dropConnectionIds = new HashSet<>();
        for (int index : subsequence) {
            dropConnectionIds.add(newConnectionIds.get(index));
        }

        Function<TextFragment, TextFragment> process = fragment ->
            fragment.getConnectionId() != null && dropConnectionIds.contains(fragment.getConnectionId())
                ? fragment.withConnectionId(null)
                : fragment;

        return new SignatureChangePresentationModel(map(myOldSignature, process), map(myNewSignature, process));
    }

    // we don't show single parameter move if there are no other fragments marked as modified
    private SignatureChangePresentationModel dropObviousSingleConnection() {
        Object singleConnectionId = null;
        int count = 0;
        for (TextFragment fragment : myOldSignature) {
            if (fragment.getConnectionId() != null) {
                singleConnectionId = fragment.getConnectionId();
                count++;
            }
        }
        if (count != 1) {
            return this;
        }

        if (hasModificationsOutsideConnection(myOldSignature, singleConnectionId)
            || hasModificationsOutsideConnection(myNewSignature, singleConnectionId)) {
            return this;
        }

        Function<TextFragment, TextFragment> process = fragment ->
            fragment.getConnectionId() != null ? fragment.withConnectionId(null) : fragment;

        return new SignatureChangePresentationModel(map(myOldSignature, process), map(myNewSignature, process));
    }

    private static boolean hasModificationsOutsideConnection(List<TextFragment> fragments, Object connectionId) {
        for (TextFragment fragment : fragments) {
            if (!connectionId.equals(fragment.getConnectionId()) && hasModifications(fragment)) {
                return true;
            }
        }
        return false;
    }

    private SignatureChangePresentationModel addMovedEffectForAllConnections() {
        return new SignatureChangePresentationModel(
            map(myOldSignature, SignatureChangePresentationModel::addMovedEffectIfConnected),
            map(myNewSignature, SignatureChangePresentationModel::addMovedEffectIfConnected)
        );
    }

    private static TextFragment addMovedEffectIfConnected(TextFragment fragment) {
        if (fragment.getConnectionId() == null) {
            return fragment;
        }
        if (fragment.getEffect() != Effect.None) {
            return fragment;
        }
        return fragment.withEffect(Effect.Moved);
    }

    private static boolean hasModifications(TextFragment fragment) {
        return fragment.anySelfOrDescendants(it -> it.getEffect() != Effect.None);
    }

    private static List<TextFragment> map(List<TextFragment> fragments, Function<TextFragment, TextFragment> function) {
        List<TextFragment> result = new ArrayList<>(fragments.size());
        for (TextFragment fragment : fragments) {
            result.add(function.apply(fragment));
        }
        return result;
    }
}
