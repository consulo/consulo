//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// This file is a part of the 'coroutines' project.
// Copyright 2018 Elmar Sonnenschein, esoco GmbH, Flensburg, Germany
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//	  http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
package consulo.util.concurrent.coroutine;

import java.util.Objects;

/**
 * This is a marker interface for identifiers of {@link Channel channels} which
 * are used for communication between {@link Coroutine coroutines}. One possible
 * usage would be to implement it on enums or similar constants that are to be
 * used as channel IDs. To simplify typed lookup of channels IDs are generically
 * typed with the datatype of the target channel. Either implementations or
 * applications are responsible that their channel IDs are unique also with
 * respect to the datatype.
 *
 * <p>
 * Instances of a default implementation of string-based IDs can be created
 * through the generic factory method {@link #channel(String, Class)}. Please
 * check the method comment for informations about the constraints imposed by
 * that implementation. There are also some derived factory methods for common
 * datatypes like {@link #stringChannel(String)}.
 * </p>
 *
 * @author eso
 */
public interface ChannelId<T> {

	/**
	 * Creates a channel ID with a boolean datatype.
	 *
	 * @param id The ID string
	 * @return The new boolean ID
	 * @see #channel(String, Class)
	 */
	static ChannelId<Boolean> booleanChannel(String id) {
		return channel(id, Boolean.class);
	}

	/**
	 * Creates a new channel ID from an identifier string. Channel IDs with the
	 * same identifier string are considered equal and will therefore give
	 * access to the same channel in a {@link CoroutineContext}. IDs aren't
	 * cached so that invocations with the the same string and datatyoe will
	 * return different instances which are considered equal (see below). To
	 * avoid name clashes in complex scenarios the ID names should be selected
	 * appropriately, e.g. by using namespaces. The implementation doesn't
	 * impose any restrictions on the strings used to define IDs.
	 *
	 * <p>
	 * Equality of string-based IDs is also based on the datatype. That means
	 * that it is possible to create equal-named channel IDs for different
	 * datatypes. Accessing channels through such IDs would yield different
	 * channel instances but that usage is not recommended. It lies in the
	 * responsibility of the application to name channel IDs appropriately for
	 * the respective context (or provide it's own {@link ChannelId}
	 * implementations).
	 * </p>
	 *
	 * @param id       The channel ID string
	 * @param datatype The class of the channel datatype to ensure
	 * @return A new channel ID
	 */
	static <T> ChannelId<T> channel(String id, Class<T> datatype) {
		Objects.requireNonNull(id);
		Objects.requireNonNull(datatype);

		return new StringId<>(id, datatype);
	}

	/**
	 * Creates a channel ID with an integer datatype.
	 *
	 * @param id The ID string
	 * @return The new integer ID
	 * @see #channel(String, Class)
	 */
	static ChannelId<Integer> intChannel(String id) {
		return channel(id, Integer.class);
	}

	/**
	 * Creates a channel ID with a string datatype.
	 *
	 * @param id The ID string
	 * @return The new string ID
	 * @see #channel(String, Class)
	 */
	static ChannelId<String> stringChannel(String id) {
		return channel(id, String.class);
	}

	/**
	 * Internal implementation of string-based channel IDs.
	 *
	 * @author eso
	 */
	class StringId<T> implements ChannelId<T> {

		private final String id;

		private final Class<T> datatype;

		/**
		 * Creates a new instance.
		 *
		 * @param id       The ID string
		 * @param datatype The channel datatype
		 */
		StringId(String id, Class<T> datatype) {
			this.id = id;
			this.datatype = datatype;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null || getClass() != obj.getClass()) {
				return false;
			}
			StringId<?> other = (StringId<?>) obj;

			return datatype == other.datatype && id.equals(other.id);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public int hashCode() {
			return 17 * datatype.hashCode() + id.hashCode();
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public String toString() {
			return String.format("%s<%s>", id, datatype.getSimpleName());
		}
	}
}
