#ifndef FIREBASE_FIRESTORE_SRC_ANDROID_JNI_DEMO_ANDROID_H_
#define FIREBASE_FIRESTORE_SRC_ANDROID_JNI_DEMO_ANDROID_H_

#include "firestore/src/jni/object.h"
#include "firestore/src/jni/jni_fwd.h"

namespace firebase {
namespace firestore {

class JniDemo : public jni::Object {
  using Object = jni::Object;

 public:
  using Object::Object;

  static void Initialize(jni::Loader&);

  static jni::Local<JniDemo> Create(jni::Env&);

  int32_t Put(jni::Env&, const Object&) const;

  jni::Local<Object> Get(jni::Env&, int32_t) const;

  int32_t Size(jni::Env&) const;
};

}  // namespace firestore
}  // namespace firebase

#endif  // FIREBASE_FIRESTORE_SRC_ANDROID_JNI_DEMO_ANDROID_H_
