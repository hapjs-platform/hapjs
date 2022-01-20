/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

#ifndef J2V8_JSENV_H_
#define J2V8_JSENV_H_

#include <dlfcn.h>

#include <cstdint>
#include <cstdlib>
#include <cstring>
#include <cwchar>

#define JSENV_VERSION 1100

#define JSENV_ENTRY "get_jsenv"

#define JSENV_SO_NAME "JSENV_SO_NAME"
#define JSENV_DEFAULT_SO_NAME "libjsenv.so"

namespace hybrid {

class JSEnv;

typedef void* J2V8Handle;

typedef wchar_t jschar_t;

typedef JSEnv* (*get_jsenv_cb)(J2V8Handle, int);

typedef struct JSObject_* JSObject;
typedef struct JSClass_* JSClass;
typedef struct J2V8ObjectHandle_* J2V8ObjectHandle;

template <typename TCHAR>
static TCHAR* tstrndup(const TCHAR* str, int len) {
  if (str == nullptr) {
    return nullptr;
  }
  TCHAR* buff = reinterpret_cast<TCHAR*>(malloc((len + 1) * sizeof(TCHAR)));
  memcpy(buff, str, len * sizeof(TCHAR));
  buff[len] = 0;
  return buff;
}

struct JSValue {
  enum {
    kNull,
    kInt,
    kUInt,
    kFloat,
    kBoolean,
    kUTF8String,
    kUTF16String,
    kJSObject,
    kJSFunction,
    kJSArray,
    kJSTypedArray,
    kJSArrayBuffer,
    kJSDataView,
    kJSPromise,
    kJSResolver,
    kUint = kUInt,  // renmae kUInt as kUint
  };

  enum {
    kJSNotTypedArray,
    kJSInt8Array,
    kJSInt16Array,
    kJSInt32Array,
    kJSInt64Array,
    kJSUint8Array,
    kJSUint8ClampedArray,
    kJSUint16Array,
    kJSUint32Array,
    kJSUint64Array,
    kJSFloat32Array,
    kJSFloat64Array,
  };

  int Type() const { return type; }
  uint32_t Length() const { return length; }

  bool IsUTF8String() const { return type == kUTF8String; }
  bool IsUTF16String() const { return type == kUTF16String; }
  bool IsString() const { return IsUTF8String() || IsUTF16String(); }
  bool IsInt() const { return type == kInt; }
  bool IsUint() const { return type == kUInt; }
  bool IsInteger() const { return IsInt() || IsUint(); }
  bool IsFloat() const { return type == kFloat; }
  bool IsNumber() const { return IsFloat() || IsInteger(); }
  bool IsBoolean() const { return type == kBoolean; }
  bool IsObject() const {
    return type == kJSObject || type == kJSFunction || type == kJSArray ||
           type == kJSTypedArray || type == kJSArrayBuffer ||
           type == kJSDataView || type == kJSPromise || type == kJSResolver;
  }

  int IntVal() const { return data.ival; }
  uint32_t UintVal() const { return data.uval; }
  double FloatVal() const { return data.fval; }
  const jschar_t* UTF16Str() const { return data.utf16_str; }
  const char* UTF8Str() const { return data.utf8_str; }
  JSObject Object() const { return data.object; }
  bool Boolean() const { return data.bval; }

  JSValue() : type(kNull), need_free(0) {}

  template <typename TCHAR>
  JSValue(const TCHAR* str, int len = -1) : type(kNull), need_free(0) {
    Set(str, len);
  }

  template <typename T>
  JSValue(T value) : type(kNull), need_free(0) {
    Set(value);
  }

  template <typename T>
  JSValue& operator=(T tval) {
    Set(tval);
  }

  void SetNull() {
    AutoFree();
    type = kNull;
    length = 0;
    need_free = false;
  }

  void Set(int ival) {
    AutoFree();
    data.ival = ival;
    type = kInt;
    length = sizeof(int);
    need_free = false;
  }

  void Set(uint32_t uval) {
    AutoFree();
    data.uval = uval;
    type = kUInt;
    length = sizeof(uint32_t);
    need_free = false;
  }

  void Set(bool bval) {
    AutoFree();
    data.bval = bval;
    type = kBoolean;
    length = sizeof(int);
    need_free = false;
  }

  void Set(double dval) {
    AutoFree();
    data.fval = dval;
    type = kFloat;
    length = sizeof(double);
    need_free = false;
  }

  void Set(const jschar_t* utf16, int len = -1, bool need_copy = false) {
    AutoFree();
    type = kUTF16String;
    if (len < 0) {
      length = utf16 ? static_cast<uint32_t>(wcslen(utf16)) : 0;
    } else {
      length = len;
    }
    if (need_copy) {
      data.utf16_str = tstrndup(utf16, length);
    } else {
      data.utf16_str = utf16;
    }
    need_free = need_copy;
  }

  void Set(const char* utf8, int len = -1, bool need_copy = false) {
    AutoFree();
    type = kUTF8String;
    if (len < 0) {
      length = utf8 ? static_cast<uint32_t>(strlen(utf8)) : 0;
    } else {
      length = len;
    }
    if (need_copy) {
      data.utf8_str = tstrndup(utf8, length);
    } else {
      data.utf8_str = utf8;
    }
    need_free = need_copy;
  }

  void Set(JSObject jsobj, int type = kJSObject) {
    AutoFree();
    data.object = jsobj;
    this->type = type;
    length = sizeof(JSObject);
    need_free = false;
  }

  ~JSValue() { AutoFree(); }

  int type : 24;
  int need_free : 8;
  uint32_t length;
  union {
    bool bval;
    int ival;
    uint32_t uval;
    double fval;
    const jschar_t* utf16_str;  // utf16 string
    const char* utf8_str;       // utf16 string
    JSObject object;
  } data;

 private:
  void AutoFree() {
    if (need_free && data.utf8_str != nullptr) {
      free(const_cast<void*>(reinterpret_cast<const void*>((data.utf8_str))));
      data.utf8_str = nullptr;
    }
    need_free = false;
  }
};

enum {
  kJSPromiseStateNoState = -1,
  kJSPromiseStatePending,
  kJSPromiseStateFulfilled,
  kJSPromiseStateRejected
};

struct JSException {
  enum { kNoneException, kJSException, kNativeException };

  JSException() : type(kNoneException), message(nullptr) {}

  JSException(const JSException& e) : type(e.type), message(e.message) {}

  JSException(int type, const char* message) : type(type), message(message) {}

  int type;
  const char* message;
};

typedef bool (*UserFunctionCallback)(JSEnv*, void* user_data,
                                     J2V8ObjectHandle handle,
                                     const JSValue* argv, int argc,
                                     JSValue* presult);

typedef void (*JSWeakReferenceCallback)(const void* weak_data);

using JSArrayBufferReleaseExteranlCallback = JSWeakReferenceCallback;

///////////////////////////////////////
// define the class

typedef bool (*JSFunctionCallback)(JSEnv* env, void* user_data, JSObject self,
                                   const JSValue* argv, int argc,
                                   JSValue* presult);

typedef bool (*JSPropertyGetCallback)(JSEnv* env, void* user_data,
                                      JSObject self, JSValue* pvalue);

typedef bool (*JSPropertySetCallback)(JSEnv* env, void* user_data,
                                      JSObject self, const JSValue* pvalue);

typedef void (*JSFinalizeCallback)(void* private_data, void* extra_data);

typedef struct {
  const char* name;
  JSFunctionCallback function;
  void* user_data;
  uint32_t flags;
} JSFunctionDefinition;

typedef struct {
  const char* name;
  JSPropertyGetCallback getter;
  JSPropertySetCallback setter;
  void* user_data;
  uint32_t flags;
} JSPropertyDefinition;

typedef struct {
  const char* class_name;            // allow null
  JSFunctionDefinition constructor;  // name is ignored
  JSFinalizeCallback finalize;

  // end with {0, 0}
  JSPropertyDefinition* properties;

  // end with {0, 0}
  JSFunctionDefinition* functions;
} JSClassDefinition;

///////////////////////////////////////////////////////
// inspector
class JSInspectorSession {
 public:
  virtual ~JSInspectorSession() {}

  virtual void DispatchProtocolMessage(const jschar_t* message,
                                       size_t size) = 0;

  virtual void OnFrontendReload() = 0;

  // for future
  virtual void* DispatchSessionCommand(int cmd, void* data) = 0;

  virtual bool CanDispatchMethod(const JSValue* method) = 0;

  virtual bool GetStateJSON(JSValue* state, uint32_t flags) = 0;

  virtual void SchedulePauseOnNextStatement(const JSValue* break_reason,
                                            const JSValue* break_details) = 0;
  virtual void CancelPauseOnNextStatement() = 0;
  virtual void BreakProgram(const JSValue* break_reason,
                            const JSValue* break_details) = 0;
  virtual void SetSkipAllPauses(bool) = 0;
  virtual void Resume() = 0;
  virtual void StepOver() = 0;
};

// imlement by user
class JSInspectorClient {
 public:
  virtual ~JSInspectorClient() {}

  virtual void SendResponse(int call_id, const jschar_t* message,
                            size_t size) = 0;
  virtual void SendNotification(const jschar_t* message, size_t size) = 0;

  // Client
  virtual void RunMessageLoopOnPause(int context_group_id) = 0;
  virtual void QuitMessageLoopOnPause() = 0;
  virtual void RunIfWaitingForDebugger(int contextGroupId) = 0;

  virtual void MuteMetrics(int contextGroupId) = 0;
  virtual void UnmuteMetrics(int contextGroupId) = 0;

  virtual void BeginUserGesture() = 0;
  virtual void EndUserGesture() = 0;
  virtual void BeginEnsureAllContextsInGroup(int contextGroupId) = 0;
  virtual void EndEnsureAllContextsInGroup(int contextGroupId) = 0;
};

class JSEnv {
 public:
  enum { kFlagUseUTF8 = 1 };

  virtual int GetVersion() const = 0;

  virtual void* DispatchJSEnvCommand(int cmd, void* data) = 0;

  virtual bool HasException() const = 0;
  virtual JSException GetException() const = 0;
  virtual void SetException(JSException exception) = 0;
  virtual void ClearException() = 0;
  void SetException(int type, const char* message) {
    SetException(JSException(type, message));
  }

  virtual JSInspectorSession* CreateInspectorSession(
      JSInspectorClient* client, int context_group_id, const char* state,
      int is_jscontext_recreated) = 0;

  // object: nullptr 代表全局
  // 其他值，来自j2v8 内的 V8Object 的 nativeHandle
  virtual bool RegisterCallbackOnObject(J2V8ObjectHandle object,
                                        const char* domain,
                                        UserFunctionCallback callback,
                                        void* user_data, uint32_t flags) = 0;

  virtual bool ExecuteScript(const JSValue* code, JSValue* presult,
                             const char* file_name = nullptr,
                             int start_lineno = 0,
                             uint32_t flags = kFlagUseUTF8) = 0;
  template <typename TCHAR>
  bool ExecuteScript(const TCHAR* code, int code_size, JSValue* presult,
                     const char* file_name = nullptr, int start_lineno = 0,
                     uint32_t flags = 0) {
    JSValue code_value;
    code_value.Set(code, code_size);
    return ExecuteScript(&code_value, presult, file_name, start_lineno, flags);
  }

  // version 1100
  // support j2v8 object handle
  virtual JSObject J2V8ObjectHandleToJSObject(J2V8ObjectHandle j2v8_handle) = 0;
  // class support
  virtual JSClass CreateClass(const JSClassDefinition* class_definition,
                              JSClass super) = 0;
  virtual JSClass GetClass(const char* class_name) = 0;
  virtual JSObject NewInstance(JSClass clazz) = 0;
  virtual JSObject NewInstanceWithConstructor(JSClass clazz,
                                              const JSValue* args,
                                              int argc) = 0;
  virtual JSObject GetClassConstructorFunction(JSClass clazz) = 0;

  // object type
  virtual int GetObjectType(JSObject object) = 0;
  virtual int GetTypedArrayType(JSObject object) = 0;

  // object reference
  virtual JSObject NewObjectReference(JSObject object) = 0;
  virtual JSObject NewObjectWeakReference(
      JSObject object, JSWeakReferenceCallback weak_callback = nullptr,
      void* user_data = nullptr) = 0;
  virtual void DeleteObjectReference(JSObject object) = 0;

  virtual void GetWeakReferenceCallbackInfo(const void* weak_data,
                                            void** p_user_data,
                                            void** p_internal_fields) = 0;

  // private data access
  virtual void* GetObjectPrivateData(JSObject object) = 0;
  virtual bool SetObjectPrivateData(JSObject object, void* user_data) = 0;
  virtual void* GetObjectPrivateExtraData(JSObject object) = 0;
  virtual bool SetObjectPrivateExtraData(JSObject object, void* user_data) = 0;

  // global object
  virtual JSObject GetGlobalObject() = 0;
  template <typename TKey, typename TValue>
  bool SetGlobalValue(const TKey& key, const TValue& value) {
    JSValue jskey(key);
    JSValue jsvalue(value);
    return SetGlobal(&jskey, &jsvalue);
  }
  template <typename TKey>
  bool GetGlobalValue(const TKey& key, JSValue* pvalue, uint32_t flags = 0) {
    JSValue jskey(key);
    return GetGlobal(&jskey, pvalue, flags);
  }
  virtual bool SetGlobal(const JSValue* pkey, const JSValue* pvalue) = 0;
  virtual bool GetGlobal(const JSValue* pkey, JSValue* pvale,
                         uint32_t flags = 0) = 0;

  // object property
  virtual bool GetObjectProperty(JSObject object, const JSValue* pkey,
                                 JSValue* pvalue, uint32_t flags = 0) = 0;
  virtual bool SetObjectProperty(JSObject object, const JSValue* pkey,
                                 const JSValue* pvalue) = 0;
  template <typename TKey>
  bool GetObjectPropertyValue(JSObject object, const TKey& key, JSValue* pval,
                              uint32_t flags = 0) {
    JSValue jskey(key);
    return GetObjectProperty(object, &jskey, pval, flags);
  }
  template <typename TKey, typename TValue>
  bool SetObjectPropertyValue(JSObject object, const TKey& key,
                              const TValue& value) {
    JSValue jskey(key);
    JSValue jsvalue(value);
    return SetObjectProperty(object, &jskey, &jsvalue);
  }

  virtual JSObject GetObjectPropertyNames(JSObject object) = 0;

  // array access
  virtual size_t GetObjectLength(JSObject object) = 0;
  virtual bool GetObjectAtIndex(JSObject object, int index, JSValue* pvalue,
                                uint32_t flags = 0) = 0;
  virtual bool SetObjectAtIndex(JSObject object, int index,
                                const JSValue* pvalue) = 0;
  template <typename TValue>
  bool SetObjectAtIndexValue(JSObject object, int index, const TValue& value) {
    JSValue jsvalue(value);
    return SetObjectAtIndex(object, index, &jsvalue);
  }

  // function call
  virtual JSObject NewFunction(JSFunctionCallback callback, void* user_data,
                               uint32_t flags = 0) = 0;

  virtual bool CallFunction(JSObject function, JSObject self,
                            const JSValue* argv, int argc, JSValue* presult,
                            uint32_t flags = 0) = 0;
  virtual JSObject CallFunctionAsConstructor(JSObject function,
                                             const JSValue* argv, int argc) = 0;

  // object create
  virtual JSObject NewObject() = 0;
  virtual JSObject NewArray(size_t length) = 0;
  virtual JSObject NewArrayWithValues(const JSValue* argv, int argc) = 0;

  // TypedArray access
  virtual JSObject NewTypedArray(int array_type, size_t element_count) = 0;
  virtual JSObject NewTypedArrayArrayBuffer(int array_type,
                                            JSObject array_buffer,
                                            size_t element_offset,
                                            size_t element_count) = 0;
  virtual size_t GetTypedArrayCount(JSObject typed_array) = 0;
  virtual void* GetTypedArrayPointer(JSObject typed_array,
                                     size_t element_offset) = 0;
  virtual JSObject GetTypedArrayArrayBuffer(JSObject typed_array) = 0;

  // ArrayBuffer access
  virtual JSObject NewArrayBuffer(size_t length) = 0;
  virtual JSObject NewArrayBufferExternal(
      void* byte, size_t length,
      JSArrayBufferReleaseExteranlCallback release_callback,
      void* user_data) = 0;
  virtual size_t GetArrayBufferLength(JSObject object) = 0;
  virtual void* GetArrayBufferPointer(JSObject object,
                                      size_t* plength = nullptr) = 0;

  // promise
  virtual JSObject CreateResolver() = 0;
  virtual JSObject GetPromiseFromResolver(JSObject resolver) = 0;
  virtual bool Resolve(JSObject object, const JSValue* pvalue) = 0;
  template <typename TValue>
  bool ResolveValue(JSObject object, const TValue& value) {
    JSValue jsvalue(value);
    return Resolve(object, &jsvalue);
  }
  virtual bool Reject(JSObject object, const JSValue* pvalue) = 0;
  template <typename TValue>
  bool RejectValue(JSObject object, const TValue& value) {
    JSValue jsvalue(value);
    return Reject(object, &jsvalue);
  }

  virtual bool SetPromiseThen(JSObject promise, JSObject function) = 0;
  virtual bool SetPromiseCatch(JSObject promise, JSObject function) = 0;
  virtual bool PromiseHasHandler(JSObject promise) = 0;
  virtual int GetPromiseState(JSObject promise) = 0;
  virtual bool GetPromiseResult(JSObject promise, JSValue* pvalue,
                                uint32_t flags = 0) = 0;

  // scope
  virtual void PushScope() = 0;
  virtual void PopScope() = 0;

  // instance
  static JSEnv* GetInstance(J2V8Handle handle, int version = JSENV_VERSION,
                            void* j2v8_so_handle = nullptr) {
    if (j2v8_so_handle == nullptr) {
      const char* jsenv_so_name = getenv(JSENV_SO_NAME);
      if (!jsenv_so_name) {
        jsenv_so_name = JSENV_DEFAULT_SO_NAME;
      }

      j2v8_so_handle = dlopen(jsenv_so_name, RTLD_NOW);
    }

    get_jsenv_cb get_env =
        reinterpret_cast<get_jsenv_cb>(dlsym(j2v8_so_handle, JSENV_ENTRY));

    if (!get_env) {
      return nullptr;
    }

    JSEnv* env = get_env(handle, version);
    if (env) {
      env->AddReference();
    }
    return env;
  }

  static void Release(JSEnv* env) {
    if (env) {
      env->Release();
    }
  }

  virtual void AddReference() = 0;
  virtual void Release() = 0;

 protected:
  virtual ~JSEnv() {}
};

}  // namespace hybrid

#endif  // J2V8_JSENV_H_
