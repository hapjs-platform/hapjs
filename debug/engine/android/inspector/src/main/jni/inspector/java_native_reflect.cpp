/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */
#include "java_native_reflect.h"

#include <stdio.h>
#include <string.h>

typedef void jvoid;

#define FieldClassName "org/hapjs/inspector/reflect/Field"
#define MethodClassName "org/hapjs/inspector/reflect/Method"

#define JNI_PRIMITIVE_ACCESS(V)     \
  V(boolean, Boolean, Boolean, "Z") \
  V(byte, Byte, Byte, "B")          \
  V(char, Char, Character, "C")     \
  V(short, Short, Short, "S")       \
  V(int, Int, Integer, "I")         \
  V(long, Long, Long, "J")          \
  V(float, Float, Float, "F")       \
  V(double, Double, Double, "D")

#define JNI_ACCESS(V)                             \
  V(object, Object, Object, "Ljava/lang/Object;") \
  JNI_PRIMITIVE_ACCESS(V)

#define DEF_PRIMITIVETYPE(BaseType, Name, ObjName, Sig)                     \
  struct Primitive##Name {                                                  \
    typedef j##BaseType base_type;                                          \
    jclass clazz;                                                           \
    jmethodID box_;                                                         \
    jmethodID unbox_;                                                       \
    Primitive##Name() : clazz(0), box_(0), unbox_(0) {}                     \
    static char getSig() { return Sig[0]; };                                \
    void init(JNIEnv* env) {                                                \
      clazz =                                                               \
          (jclass)env->NewGlobalRef(env->FindClass("java/lang/" #ObjName)); \
      box_ = env->GetStaticMethodID(clazz, "valueOf",                       \
                                    "(" Sig ")Ljava/lang/" #ObjName ";");   \
      unbox_ = env->GetMethodID(clazz, #BaseType "Value", "()" Sig);        \
    }                                                                       \
    jobject box(JNIEnv* env, base_type value) {                             \
      return env->CallStaticObjectMethod(clazz, box_, value);               \
    }                                                                       \
    base_type unbox(JNIEnv* env, jobject value) {                           \
      return env->Call##Name##Method(value, unbox_);                        \
    }                                                                       \
  };                                                                        \
  static Primitive##Name sPrimitive##Name;

#define jvalue_set(jtype, C) \
  static inline void set(jvalue& value, jtype v) { value.C = v; }

jvalue_set(jchar, c)

jvalue_set(jboolean, z)

jvalue_set(jbyte, b)

jvalue_set(jshort, s)

jvalue_set(jint, i)

jvalue_set(jlong, j)

jvalue_set(jdouble, d)

jvalue_set(jfloat, f)

jvalue_set(jobject, l)

JNI_PRIMITIVE_ACCESS(DEF_PRIMITIVETYPE)

static void InitPrimitiveTypes(JNIEnv *env) {
#define PRIMITIVE_INIT(BaseType, Name, ObjName, Sig) sPrimitive##Name.init(env);
    JNI_PRIMITIVE_ACCESS(PRIMITIVE_INIT)
}

static void primitive_unbox(JNIEnv *env, jobject objValue, jvalue &value) {
    jclass objclass = env->GetObjectClass(objValue);
#define PRIMITIVE_UNBOX(BaseType, Name, ObjName, Sig)        \
  if (env->IsSameObject(sPrimitive##Name.clazz, objclass)) { \
    set(value, sPrimitive##Name.unbox(env, objValue));       \
    return;                                                  \
  }

    JNI_PRIMITIVE_ACCESS(PRIMITIVE_UNBOX)

    set(value, objValue);
}

struct java_string {
    JNIEnv *env;
    jstring jstr;
    const char *str;

    java_string(JNIEnv *env, jstring jstr) : env(env), jstr(jstr), str(NULL) {}

    const char *operator*() {
        if (str == NULL) str = env->GetStringUTFChars(jstr, NULL);
        return str;
    }

    ~java_string() {
        if (str && jstr) {
            env->ReleaseStringUTFChars(jstr, str);
        }
    }
};

template<typename T>
struct JAccessWrapper {
    typedef T jtype;

    jlong value_;

    JAccessWrapper(T v, bool isStatic) {
        value_ = (reinterpret_cast<jlong>(v) << 2)| (isStatic ? 1 : 0);
    }

    JAccessWrapper(jlong v) : value_(v) {}

    T id() const { return reinterpret_cast<T>(value_ >> 2); }

    bool isStatic() const { return value_ & 1; }

    jlong value() const { return value_; }
};

typedef JAccessWrapper<jfieldID> JFieldWrapper;
typedef JAccessWrapper<jmethodID> JMethodWrapper;

static jlong Field_nativeGetField(JNIEnv *env, jclass, jstring className,
                                  jstring fieldName, jstring signature,
                                  jboolean isstatic) {
    java_string str_classname(env, className);
    java_string str_fieldname(env, fieldName);
    java_string str_signature(env, signature);

    jclass clazz = env->FindClass(*str_classname);
    if (clazz == NULL) {
        return 0;
    }

    jfieldID id = 0;
    if (isstatic) {
        id = env->GetStaticFieldID(clazz, *str_fieldname, *str_signature);
    } else {
        id = env->GetFieldID(clazz, *str_fieldname, *str_signature);
    }

    return JFieldWrapper(id, isstatic).value();
}

#define Field_nativeGet(RetType, Name, ObjName, Sig)                          \
  static j##RetType Field_nativeGet##Name(JNIEnv* env, jclass, jlong fieldId, \
                                          jobject thiz) {                     \
    JFieldWrapper field(fieldId);                                             \
    if (field.id() != 0) {                                                    \
      if (field.isStatic())                                                   \
        return (j##RetType)(                                                  \
            env->GetStatic##Name##Field((jclass)thiz, field.id()));           \
      else                                                                    \
        return (j##RetType)(env->Get##Name##Field(thiz, field.id()));         \
    }                                                                         \
    return (j##RetType)0;                                                     \
  }

#define Field_nativeSet(RetType, Name, ObjName, Sig)                    \
  static void Field_nativeSet##Name(JNIEnv* env, jclass, jlong fieldId, \
                                    jobject thiz, j##RetType value) {   \
    JFieldWrapper field(fieldId);                                       \
    if (field.id() != 0) {                                              \
      if (field.isStatic())                                             \
        env->SetStatic##Name##Field((jclass)thiz, field.id(), value);   \
      else                                                              \
        env->Set##Name##Field(thiz, field.id(), value);                 \
    }                                                                   \
  }

#define Field_DefNativeMethod(RetType, Name, ObjName, Sig) \
  {"nativeGet" #Name, "(JLjava/lang/Object;)" Sig,         \
   (void*)Field_nativeGet##Name},                          \
      {"nativeSet" #Name, "(JLjava/lang/Object;" Sig ")V", \
       (void*)Field_nativeSet##Name},

JNI_ACCESS(Field_nativeGet)

JNI_ACCESS(Field_nativeSet)

static JNINativeMethod _field_methods[] = {
        {"nativeGetField",
         "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Z)J",
         (void *) (Field_nativeGetField)},
        JNI_ACCESS(Field_DefNativeMethod)};

static jvalue *convert_arguments(JNIEnv *env, jobjectArray arguments) {
    int length = env->GetArrayLength(arguments);
    if (length == 0) return NULL;

    jvalue *args = new jvalue[length];
    for (int i = 0; i < length; i++) {
        primitive_unbox(env, env->GetObjectArrayElement(arguments, i), args[i]);
    }
    return args;
}

#define Method_nativeInvoke(RetType, Name, ObjName, Sig)                      \
  static j##RetType Method_nativeInvoke##Name(JNIEnv* env, jclass,            \
                                              jlong methodId, jobject thiz,   \
                                              jobjectArray arguments) {       \
    JMethodWrapper method(methodId);                                          \
    if (method.id() == 0) return (j##RetType)0;                               \
    jvalue* args = convert_arguments(env, arguments);                         \
    if (method.isStatic())                                                    \
      return (j##RetType)(                                                    \
          env->CallStatic##Name##MethodA((jclass)thiz, method.id(), args));   \
    else                                                                      \
      return (j##RetType)(env->Call##Name##MethodA(thiz, method.id(), args)); \
    if (args) delete[] args;                                                  \
  }

JNI_ACCESS(Method_nativeInvoke)

Method_nativeInvoke(void, Void, Void, "V")

static jlong Method_nativeGetMethod(JNIEnv *env, jclass, jstring className,
                                    jstring methodName, jstring signature,
                                    jboolean isstatic) {
    java_string str_classname(env, className);
    java_string str_methodname(env, methodName);
    java_string str_signature(env, signature);

    jclass clazz = env->FindClass(*str_classname);
    if (clazz == NULL) {
        return 0;
    }

    jmethodID id = 0;
    if (isstatic)
        id = env->GetStaticMethodID(clazz, *str_methodname, *str_signature);
    else
        id = env->GetMethodID(clazz, *str_methodname, *str_signature);
    return JMethodWrapper(id, isstatic).value();
}

#define Method_DefNativeInvoke(RetType, Name, ObjName, Sig)              \
  {"nativeInvoke" #Name, "(JLjava/lang/Object;[Ljava/lang/Object;)" Sig, \
   (void*)(Method_nativeInvoke##Name)},

static JNINativeMethod _method_methods[] = {
        {"nativeGetMethod",
         "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Z)J",
         (void *) Method_nativeGetMethod},
        JNI_ACCESS(Method_DefNativeInvoke)
        Method_DefNativeInvoke(void, Void, Void, "V")};

bool NativeReflectInit(JNIEnv *env) {
    InitPrimitiveTypes(env);
    jclass field_class = env->FindClass(FieldClassName);
    if (env->RegisterNatives(
            field_class, _field_methods,
            sizeof(_field_methods) / sizeof(_field_methods[0]))) {
        return false;
    }

    jclass method_class = env->FindClass(MethodClassName);
    if (env->RegisterNatives(
            method_class, _method_methods,
            sizeof(_method_methods) / sizeof(_method_methods[0]))) {
        return false;
    }

    return true;
}
