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

#include <unordered_map>
#include <unordered_set>

#include "android/firestore_integration_test_android.h"

#include "firestore/src/jni/env.h"
#include "firestore/src/jni/throwable.h"
#include "firestore/src/jni/object.h"
#include "firestore/src/jni/ownership.h"
#include "firestore/src/jni/long.h"

#include "gtest/gtest.h"

namespace firebase {
namespace firestore {

namespace {

using ObjectArenaTest = FirestoreIntegrationTest;

using jni::Env;
using jni::Local;
using jni::Long;
using jni::Object;
using jni::Throwable;

TEST_F(ObjectArenaTest, CreateCreatesANewEmptyInstance) {
  Env env;

  Local<ObjectArena> object_arena1 = ObjectArena::Create(env);
  Local<ObjectArena> object_arena2 = ObjectArena::Create(env);

  ASSERT_TRUE(env.ok());
  EXPECT_EQ(object_arena1.Size(env), 0);
  EXPECT_EQ(object_arena2.Size(env), 0);
  EXPECT_NE(object_arena1, object_arena2);
}

TEST_F(ObjectArenaTest, AddReturnsAUniqueValue) {
  Env env;
  Local<ObjectArena> object_arena = ObjectArena::Create(env);
  Local<Long> long_object1 = Long::Create(env, 1);
  Local<Long> long_object2 = Long::Create(env, 2);

  int64_t key1 = object_arena.Add(env, long_object1);
  int64_t key2 = object_arena.Add(env, long_object2);

  ASSERT_TRUE(env.ok());
  EXPECT_NE(key1, key2);
}

TEST_F(ObjectArenaTest, AddCanBeInvokedThousandsOfTimes) {
  Env env;
  Local<ObjectArena> object_arena = ObjectArena::Create(env);
  Local<Long> long_object = Long::Create(env, 1);

  for (int i=0; i<100000; ++i) {
    object_arena.Add(env, long_object);
  }

  ASSERT_TRUE(env.ok());
  EXPECT_EQ(object_arena.Size(env), 100000);
}

TEST_F(ObjectArenaTest, GetReturnsTheCorrectObject) {
  Env env;
  Local<ObjectArena> object_arena = ObjectArena::Create(env);

  std::unordered_map<int64_t, int64_t> value_by_key;
  for (int i=0; i<100000; ++i) {
    Local<Long> long_object = Long::Create(env, i);
    int64_t key = object_arena.Add(env, long_object);
    value_by_key[key] = i;
  }

  ASSERT_TRUE(env.ok());
  for (const auto& value_by_key_entry : value_by_key) {
    int64_t key = value_by_key_entry.first;
    int64_t expected_value = value_by_key_entry.second;
    Local<Object> actualValue = object_arena.Get(env, key);
    Local<Long> expectedValue = Long::Create(env, expected_value);
    ASSERT_TRUE(actualValue.Equals(env, expectedValue)) << "key=" << key << " expected_value=" << expected_value;
  }
  ASSERT_TRUE(env.ok());
}

TEST_F(ObjectArenaTest, GetThrowsIfKeyIsZero) {
  Env env;
  Local<ObjectArena> object_arena = ObjectArena::Create(env);
  ASSERT_TRUE(env.ok());

  object_arena.Get(env, 0);

  ASSERT_FALSE(env.ok());
  Local<Throwable> exception = env.ClearExceptionOccurred();
  EXPECT_EQ(exception.GetMessage(env), "key is not assigned: 0");
}

TEST_F(ObjectArenaTest, GetThrowsIfKeyIsNotSet) {
  Env env;
  Local<ObjectArena> object_arena = ObjectArena::Create(env);
  std::unordered_set<int64_t> not_added_keys;
  for (int64_t i=0; i<2000; ++i) {
    not_added_keys.insert(i);
  }
  {
    Local<Long> long_object = Long::Create(env, 42);
    for (int i = 0; i < 1000; ++i) {
      int64_t added_key = object_arena.Add(env, long_object);
      not_added_keys.erase(added_key);
    }
  }
  ASSERT_TRUE(env.ok());
  ASSERT_FALSE(not_added_keys.empty());

  for (int64_t not_added_key : not_added_keys) {
    object_arena.Get(env, not_added_key);
    ASSERT_FALSE(env.ok()) << "key=" << not_added_key;
    Local<Throwable> exception = env.ClearExceptionOccurred();
    EXPECT_EQ(exception.GetMessage(env), "key is not assigned: " + std::to_string(not_added_key));
  }
}

}  // namespace
}  // namespace firestore
}  // namespace firebase
