/*
 * Copyright 2013-2025 consulo.io
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
package consulo.application.impl.internal.util;

import consulo.application.util.CachedValue;
import consulo.application.util.CachedValueProvider;
import consulo.application.util.ParameterizedCachedValue;
import consulo.application.util.ParameterizedCachedValueProvider;
import consulo.language.psi.PsiElement;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import consulo.util.dataholder.UserDataHolder;
import consulo.util.dataholder.UserDataHolderEx;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.mockito.verification.VerificationMode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * @author UNV
 * @since 2025-06-14
 */
public class CachedValuesManagerImplTest {
    @SuppressWarnings("unchecked")
    private class MyUserDataHolder implements UserDataHolder {
        public CachedValue<String> data;

        private MyUserDataHolder(CachedValue<String> data) {
            this.data = data;
        }

        @Nullable
        @Override
        public <T> T getUserData(@Nonnull Key<T> key) {
            check(key);
            return (T) data;
        }

        @Override
        public <T> void putUserData(@Nonnull Key<T> key, @Nullable T value) {
            check(key);
            data = (CachedValue) value;
        }

        @Nonnull
        @Override
        public <T> T putUserDataIfAbsent(@Nonnull Key<T> key, @Nonnull T value) {
            check(key);
            if (data == null) {
                data = (CachedValue) value;
            }
            return (T) data;
        }

        private <T> void check(Key<T> key) {
            if (key != OUR_STRING_KEY) {
                throw new IllegalArgumentException();
            }
        }
    }

    private class MyUserDataHolderEx extends MyUserDataHolder implements UserDataHolderEx {
        private MyUserDataHolderEx(CachedValue<String> data) {
            super(data);
        }

        @Override
        public <T> boolean replace(@Nonnull Key<T> key, @Nullable T oldValue, @Nullable T newValue) {
            throw new UnsupportedOperationException();
        }
    }

    private static final Key<CachedValue<String>> OUR_STRING_KEY = Key.create("CachedValuesManagerImplTest.String");

    private final Project myProject = mock(Project.class);
    private final CachedValuesFactory myFactory = mock(CachedValuesFactory.class);

    private CachedValuesManagerImpl myCachedValuesManager = new CachedValuesManagerImpl(myProject, myFactory);

    @Test
    @SuppressWarnings("unchecked")
    void testGetCachedValueGenericHit() {
        String value = "Foobar";

        CachedValue<String> cachedValue = mock(CachedValue.class);
        when(cachedValue.getValue()).thenReturn(value);

        assertThat(myCachedValuesManager.getCachedValue(new MyUserDataHolder(cachedValue), OUR_STRING_KEY, () -> null, false))
            .isEqualTo(value);

        verify(cachedValue, once()).getValue();
    }

    @Test
    @SuppressWarnings("unchecked")
    void testGetCachedValueExHit() {
        String value = "Foobar";
        CachedValueProvider<String> provider = () -> null;

        CachedValue<String> cachedValue = mock(CachedValue.class, withSettings().extraInterfaces(ProjectCachedValueEx.class));
        ProjectCachedValueEx cachedValueEx = (ProjectCachedValueEx) cachedValue;
        when(cachedValue.getValue()).thenReturn(value);
        when(cachedValue.getValueProvider()).thenReturn(provider);
        when(cachedValue.getUpToDateOrNull()).thenReturn(() -> value);
        when(cachedValueEx.isFromMyProject(myProject)).thenReturn(true);

        assertThat(myCachedValuesManager.getCachedValue(new MyUserDataHolder(cachedValue), OUR_STRING_KEY, provider, false))
            .isEqualTo(value);

        verify(cachedValueEx, once()).isFromMyProject(myProject);
        verify(cachedValue, once()).getUpToDateOrNull();
    }

    @Test
    @SuppressWarnings("unchecked")
    void testGetCachedValueGenericMiss() {
        String value = "Foobar";
        CachedValueProvider<String> provider = () -> CachedValueProvider.Result.create(value);

        MyUserDataHolder dataHolder = new MyUserDataHolder(null);

        CachedValue<String> newCachedValue = mock(CachedValue.class, withSettings().extraInterfaces(ProjectCachedValueEx.class));
        ProjectCachedValueEx newCachedValueEx = (ProjectCachedValueEx) newCachedValue;
        when(newCachedValue.getValue()).thenReturn(value);
        when(newCachedValueEx.isFromMyProject(myProject)).thenReturn(true);

        when(myFactory.createCachedValue(provider, false)).thenReturn(newCachedValue);

        assertThat(myCachedValuesManager.getCachedValue(dataHolder, OUR_STRING_KEY, provider, false))
            .isEqualTo(value);

        verify(newCachedValue, once()).getValue();
        verify(myFactory, once()).checkProviderForMemoryLeak(provider, OUR_STRING_KEY, dataHolder);
        verify(myFactory, once()).createCachedValue(provider, false);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testGetCachedValueExMiss() {
        String value = "Foobar";
        CachedValueProvider<String> provider = () -> CachedValueProvider.Result.create(value);

        CachedValue<String> storedCachedValue = mock(CachedValue.class, withSettings().extraInterfaces(ProjectCachedValueEx.class));
        ProjectCachedValueEx storedCachedValueEx = (ProjectCachedValueEx) storedCachedValue;
        when(storedCachedValueEx.isFromMyProject(myProject)).thenReturn(true);
        when(storedCachedValue.getUpToDateOrNull()).thenReturn(null);

        MyUserDataHolder dataHolder = new MyUserDataHolder(storedCachedValue);

        CachedValue<String> newCachedValue = mock(CachedValue.class, withSettings().extraInterfaces(ProjectCachedValueEx.class));
        ProjectCachedValueEx newCachedValueEx = (ProjectCachedValueEx) newCachedValue;
        when(newCachedValue.getValue()).thenReturn(value);
        when(newCachedValueEx.isFromMyProject(myProject)).thenReturn(true);

        when(myFactory.createCachedValue(provider, false)).thenReturn(newCachedValue);

        assertThatThrownBy(() -> myCachedValuesManager.getCachedValue(dataHolder, OUR_STRING_KEY, provider, false))
            .isInstanceOf(AssertionError.class);

        verify(storedCachedValueEx, once()).isFromMyProject(myProject);
        verify(storedCachedValue, once()).getUpToDateOrNull();
    }

    @Test
    @SuppressWarnings("unchecked")
    void testGetCachedValueGenericToPsiElementMissThenDesync() {
        String value = "Foobar";
        CachedValueProvider<String> provider = () -> CachedValueProvider.Result.create(value);

        CachedValue<String> cachedValue = mock(CachedValue.class, withSettings().extraInterfaces(ProjectCachedValueEx.class));
        when(cachedValue.getValue()).thenReturn(value);
        when(((ProjectCachedValueEx) cachedValue).isFromMyProject(myProject)).thenReturn(true);

        PsiElement psiElement = mock(PsiElement.class);
        when(psiElement.getUserData(any()))
            .thenReturn(null)
            .thenReturn(cachedValue);

        when(myFactory.createCachedValue(provider, false)).thenReturn(cachedValue);

        assertThat(myCachedValuesManager.getCachedValue(psiElement, OUR_STRING_KEY, provider, false))
            .isEqualTo(value);

        verify(psiElement, times(2)).getUserData(OUR_STRING_KEY);
        verify(cachedValue, once()).getValue();
        verify(myFactory, once()).checkProviderForMemoryLeak(provider, OUR_STRING_KEY, psiElement);
        verify(myFactory, once()).createCachedValue(provider, false);
        verify(psiElement, never()).putUserData(any(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testGetCachedValueGenericToUserDataHolderExMiss() {
        String value = "Foobar";
        CachedValueProvider<String> provider = () -> CachedValueProvider.Result.create(value);

        CachedValue<String> cachedValue = mock(CachedValue.class, withSettings().extraInterfaces(ProjectCachedValueEx.class));
        when(cachedValue.getValue()).thenReturn(value);
        when(((ProjectCachedValueEx) cachedValue).isFromMyProject(myProject)).thenReturn(true);

        UserDataHolderEx userDataHolderEx = new MyUserDataHolderEx(null);

        when(myFactory.createCachedValue(provider, false)).thenReturn(cachedValue);

        assertThat(myCachedValuesManager.getCachedValue(userDataHolderEx, OUR_STRING_KEY, provider, false))
            .isEqualTo(value);

        verify(cachedValue, once()).getValue();
        verify(myFactory, once()).checkProviderForMemoryLeak(provider, OUR_STRING_KEY, userDataHolderEx);
        verify(myFactory, once()).createCachedValue(provider, false);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testCreateParameterizedCachedValue() {
        String value = "Foobar";
        ParameterizedCachedValueProvider<String, String> provider = param -> CachedValueProvider.Result.create(value);

        ParameterizedCachedValue<String, String> cachedValue = mock(ParameterizedCachedValue.class);

        when(myFactory.createParameterizedCachedValue(provider, false)).thenReturn(cachedValue);

        assertThat(myCachedValuesManager.createParameterizedCachedValue(provider, false))
            .isEqualTo(cachedValue);

        verify(myFactory, once()).createParameterizedCachedValue(provider, false);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testClearCachedValues() {
        String value = "Foobar";
        CachedValueProvider<String> provider = () -> CachedValueProvider.Result.create(value);

        MyUserDataHolder dataHolder = new MyUserDataHolder(null);

        CachedValue<String> cachedValue = mock(CachedValue.class, withSettings().extraInterfaces(ProjectCachedValueEx.class));
        when(cachedValue.getValue()).thenReturn(value);
        when(((ProjectCachedValueEx) cachedValue).isFromMyProject(myProject)).thenReturn(true);

        when(myFactory.createCachedValue(provider, false)).thenReturn(cachedValue);

        assertThat(myCachedValuesManager.getCachedValue(dataHolder, OUR_STRING_KEY, provider, false))
            .isEqualTo(value);

        assertThat(dataHolder.data).isSameAs(cachedValue);

        myCachedValuesManager.clearCachedValues();

        assertThat(dataHolder.data).isNull();
    }

    private static VerificationMode once() {
        return times(1);
    }
}
