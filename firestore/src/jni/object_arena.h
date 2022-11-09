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

#ifndef FIREBASE_FIRESTORE_SRC_JNI_OBJECT_ARENA_H_
#define FIREBASE_FIRESTORE_SRC_JNI_OBJECT_ARENA_H_

#include "firestore/src/jni/jni_fwd.h"
#include "firestore/src/jni/map.h"

namespace firebase {
namespace firestore {
namespace jni {

/** A C++ proxy for a Java `HashMap`. */
class ObjectArena : public Object {
 public:
  using Object::Object;

  static void Initialize(Loader& loader);

  static const ObjectArena& GetInstance(Env& env);

  int64_t Insert(Env& env, const Object& object) const;

  void Remove(Env& env, int64_t id) const;

  Local<Object> Get(Env& env, int64_t id) const;
};

}  // namespace jni
}  // namespace firestore
}  // namespace firebase

#endif  // FIREBASE_FIRESTORE_SRC_JNI_OBJECT_ARENA_H_
