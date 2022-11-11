#include "firestore/src/android/jni_demo_android.h"

#include "firestore/src/jni/env.h"
#include "firestore/src/jni/loader.h"
#include "firestore/src/jni/declaration.h"
#include "firestore/src/jni/ownership.h"

namespace firebase {
namespace firestore {

namespace {

using jni::Constructor;
using jni::Method;
using jni::Env;
using jni::Loader;
using jni::Local;
using jni::Object;

Constructor<JniDemo> kConstructor("()V");
Method<int32_t> kPut("put", "(Ljava/lang/Object;)I");
Method<Object> kGet("get", "(I)Ljava/lang/Object;");
Method<int32_t> kSize("size", "()I");

} // namespace

void JniDemo::Initialize(Loader& loader) {
  loader.LoadClass("com/google/firebase/firestore/internal/cpp/JniDemo", kConstructor, kPut, kGet, kSize);
}

Local<JniDemo> JniDemo::Create(Env& env) {
  return env.New(kConstructor);
}

int32_t JniDemo::Put(Env& env, const Object& object) const {
  return env.Call(*this, kPut, object);
}

Local<Object> JniDemo::Get(Env& env, int32_t key) const {
  return env.Call(*this, kGet, key);
}

int32_t JniDemo::Size(Env& env) const {
  return env.Call(*this, kSize);
}

}  // namespace firestore
}  // namespace firebase
