/*
 * Copyright 2022 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef FIREBASE_FIRESTORE_SRC_ANDROID_OBJECT_ARENA_ANDROID_H_
#define FIREBASE_FIRESTORE_SRC_ANDROID_OBJECT_ARENA_ANDROID_H_

#include "firestore/src/jni/jni_fwd.h"
#include "firestore/src/jni/object.h"
#include "firestore/src/jni/ownership.h"

namespace firebase {
namespace firestore {

class ObjectArena : public jni::Object {
 public:
  using jni::Object::Object;

  static void Initialize(jni::Loader& loader);

  static jni::Local<ObjectArena> Create(jni::Env&);

  int64_t Add(jni::Env&, const jni::Object&) const;

  void Remove(jni::Env&, int64_t) const;

  jni::Local<Object> Get(jni::Env&, int64_t) const;

  int64_t Dup(jni::Env&, int64_t) const;

  int32_t Size(jni::Env&) const;
};

}  // namespace firestore
}  // namespace firebase

#endif  // FIREBASE_FIRESTORE_SRC_ANDROID_OBJECT_ARENA_ANDROID_H_
