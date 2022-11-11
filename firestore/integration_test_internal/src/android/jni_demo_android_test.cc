#include "firestore/src/android/jni_demo_android.h"

#include "android/firestore_integration_test_android.h"

#include "firestore/src/android/firestore_android.h"
#include "firestore/src/jni/env.h"
#include "firestore/src/jni/loader.h"
#include "firestore/src/jni/ownership.h"
#include "firestore/src/jni/string.h"
#include "firestore/src/jni/throwable.h"

#include "gtest/gtest.h"

namespace firebase {
namespace firestore {
namespace {

using jni::Class;
using jni::Env;
using jni::Loader;
using jni::Local;
using jni::Object;
using jni::String;
using jni::Throwable;

using JniDemoTest = FirestoreAndroidIntegrationTest;

TEST_F(JniDemoTest, Load) {
  Env env;

  Local<JniDemo> instance = JniDemo::Create(env);

  ASSERT_TRUE(instance.get() != nullptr);
}

TEST_F(JniDemoTest, Put) {
  Env env;
  Local<String> value = env.NewStringUtf("foo");
  Local<JniDemo> instance = JniDemo::Create(env);

  int32_t key1 = instance.Put(env, value);
  int32_t key2 = instance.Put(env, value);

  ASSERT_NE(key1, key2);
}

TEST_F(JniDemoTest, Get) {
  Env env;
  Local<String> value_foo = env.NewStringUtf("foo");
  Local<String> value_bar = env.NewStringUtf("bar");
  Local<JniDemo> instance = JniDemo::Create(env);
  int32_t key_foo = instance.Put(env, value_foo);
  int32_t key_bar = instance.Put(env, value_bar);

  Local<Object> result_foo = instance.Get(env, key_foo);
  Local<Object> result_bar = instance.Get(env, key_bar);

  ASSERT_TRUE(Object::Equals(env, result_foo, value_foo));
  ASSERT_TRUE(Object::Equals(env, result_bar, value_bar));
}

TEST_F(JniDemoTest, Size) {
  Env env;
  Local<JniDemo> instance = JniDemo::Create(env);
  ASSERT_EQ(instance.Size(env), 0);

  Local<String> value_foo = env.NewStringUtf("foo");
  instance.Put(env, value_foo);
  ASSERT_EQ(instance.Size(env), 1);

  Local<String> value_bar = env.NewStringUtf("bar");
  instance.Put(env, value_bar);
  ASSERT_EQ(instance.Size(env), 2);
}

TEST_F(JniDemoTest, PutThrowsNPE) {
  Env env;
  Local<JniDemo> instance = JniDemo::Create(env);

  int32_t key_foo = instance.Put(env, Object());
  ASSERT_FALSE(env.ok());
  Local<Throwable> exception = env.ClearExceptionOccurred();

  EXPECT_EQ(key_foo, 0);

  EXPECT_EQ(exception.GetMessage(env), "obj==null");
  Local<Class> exceptionClass = env.GetObjectClass(exception);
  EXPECT_EQ(exceptionClass.GetName(env), "java.lang.NullPointerException");
}

}  // namespace
}  // namespace firestore
}  // namespace firebase
