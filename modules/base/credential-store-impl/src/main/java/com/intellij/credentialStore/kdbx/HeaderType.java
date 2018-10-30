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
 * @author VISTALL
 * @since 2018-10-27
 */
interface HeaderType {
  int END = 0;
  int COMMENT = 1;
  int CIPHER_ID = 2;
  int COMPRESSION_FLAGS = 3;
  int MASTER_SEED = 4;
  int TRANSFORM_SEED = 5;
  int TRANSFORM_ROUNDS = 6;
  int ENCRYPTION_IV = 7;
  int PROTECTED_STREAM_KEY = 8;
  int STREAM_START_BYTES = 9;
  int INNER_RANDOM_STREAM_ID = 10;
}
