/*
 * Copyright 2013-2018 consulo.io
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
package com.intellij.credentialStore.kdbx;

/**
 * This class represents the header portion of a KeePass KDBX file or stream. The header is received in
 * plain text and describes the encryption and compression of the remainder of the file.
 * It is a factory for encryption and decryption streams and contains a hash of its own serialization.
 * While KDBX streams are Little-Endian, data is passed to and from this class in standard Java byte order.
 *
 * @author jo
 *
 * from kotlin
 */
public class KdbxHeader {
}
