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

#include "firestore/src/android/object_arena_android.h"

#include "firestore/src/jni/declaration.h"
#include "firestore/src/jni/env.h"
#include "firestore/src/jni/loader.h"
#include "firestore/src/jni/object.h"
#include "firestore/src/jni/ownership.h"

namespace firebase {
namespace firestore {

using jni::Env;
using jni::Loader;
using jni::Local;
using jni::Object;

namespace {

using jni::Constructor;
using jni::Method;

constexpr char kObjectArenaClassName[] =
    "com/google/firebase/firestore/internal/cpp/ObjectArena";
Constructor<ObjectArena> kConstructor("()V");
Method<jlong> kAdd("add", "(Ljava/lang/Object;)J");
Method<void> kRemove("remove", "(J)V");
Method<Object> kGet("get", "(J)Ljava/lang/Object;");
Method<jlong> kDup("dup", "(J)J");
Method<jint> kSize("size", "()I");

}  // namespace

void ObjectArena::Initialize(Loader& loader) {
  loader.LoadClass(kObjectArenaClassName, kConstructor, kAdd, kRemove, kGet,
                   kDup, kSize);
}

Local<ObjectArena> ObjectArena::Create(Env& env) {
  return env.New(kConstructor);
}

int64_t ObjectArena::Add(Env& env, const Object& object) const {
  return env.Call(*this, kAdd, object);
}

void ObjectArena::Remove(Env& env, int64_t key) const {
  env.Call(*this, kRemove, key);
}

Local<Object> ObjectArena::Get(Env& env, int64_t key) const {
  return env.Call(*this, kGet, key);
}

int64_t ObjectArena::Dup(Env& env, int64_t key) const {
  return env.Call(*this, kDup, key);
}

int32_t ObjectArena::Size(Env& env) const { return env.Call(*this, kSize); }

}  // namespace firestore
}  // namespace firebase
