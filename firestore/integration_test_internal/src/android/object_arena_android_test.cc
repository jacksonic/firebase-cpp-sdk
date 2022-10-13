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

#include "android/firestore_integration_test_android.h"

#include "firestore/src/jni/env.h"
#include "firestore/src/jni/ownership.h"

#include "gtest/gtest.h"

namespace firebase {
namespace firestore {

namespace {

using jni::Env;
using jni::Local;

TEST(ObjectArenaTest, CreateCreatesANewEmptyInstance) {
  Env env;

  Local<ObjectArena> object_arena1 = ObjectArena::Create(env);
  Local<ObjectArena> object_arena2 = ObjectArena::Create(env);

  EXPECT_EQ(object_arena1.Size(env), 0);
  EXPECT_EQ(object_arena2.Size(env), 0);
  EXPECT_NE(object_arena1, object_arena2);
}

}  // namespace
}  // namespace firestore
}  // namespace firebase
