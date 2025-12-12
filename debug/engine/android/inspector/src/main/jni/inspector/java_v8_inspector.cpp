/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */
#include "java_v8_inspector.h"

#include <stdlib.h>

#include <map>
#include <string>
#include <vector>

#include "inspector-log.h"
#include "java_native_reflect.h"

JavaVM *g_jvm;

#define CONTEXT_GROUP_ID 0x1
#define TAG "HYBRID_INSPECTOR"

static inline JNIEnv *AttachCurrentThread() {
    JNIEnv *env = NULL;
    g_jvm->AttachCurrentThread(&env, NULL);
    return env;
}

namespace inspector {

/////////////////////////////////////////////////////////////
#define V8InspectorClassName "org/hapjs/render/jsruntime/V8InspectorNative"

    static jstring toJavaString(JNIEnv *env, const hybrid::jschar_t *str,
                                size_t size) {
        if (str == NULL || size <= 0) {
            return NULL;
        }
        return env->NewString(reinterpret_cast<const jchar *>(str), size);
    }

    static std::string jstringTostring(JNIEnv *env, jstring jstr) {
        const char *str = env->GetStringUTFChars(jstr, NULL);
        std::string ret_str(str);
        env->ReleaseStringUTFChars(jstr, str);
        return ret_str;
    }

/////////////////////////////////////////////////////////////////////
//////////////////    class InspectorClientImpl      ////////////////
/////////////////////////////////////////////////////////////////////
    void InspectorClientImpl::SendResponse(int call_id,
                                           const hybrid::jschar_t *message,
                                           size_t size) {
        if (inspector_) {
            inspector_->sendMessageToFrontend(call_id, message, size);
        }
    }

    void InspectorClientImpl::RunMessageLoopOnPause(int context_group_id) {
        if (inspector_) {
            inspector_->runMessageLoopOnPause(context_group_id);
        }
    }

    void InspectorClientImpl::QuitMessageLoopOnPause() {
        if (inspector_) {
            inspector_->quitMessageLoopOnPause();
        }
    }

////////////////////////////////////////////////////////////////////
//////////////// class HybridInspector  ////////////////////////////
////////////////////////////////////////////////////////////////////

    void HybridInspector::handleMessage(int sessionId,
                                        const hybrid::jschar_t *message,
                                        size_t size) {
        m_sessionId = sessionId;
        if (inspector_client_ && inspector_client_->GetSession()) {
            JNIEnv *env = AttachCurrentThread();
            jstring jstr = toJavaString(env, message, size);
            inspector_client_->GetSession()->DispatchProtocolMessage(message, size);
        }
    }

    void HybridInspector::onFrontendReload() {
        if (inspector_client_ && inspector_client_->GetSession()) {
            inspector_client_->GetSession()->OnFrontendReload();
        }
    }

    void HybridInspector::runMessageLoopOnPause(int contextGroupId) {
        JNIEnv *env = AttachCurrentThread();
        env->CallVoidMethod(m_jthis, _runMessageLoopOnPause, contextGroupId);
    }

    void HybridInspector::quitMessageLoopOnPause() {
        JNIEnv *env = AttachCurrentThread();
        env->CallVoidMethod(m_jthis, _quitMessageLoopOnPause);
    }

    void HybridInspector::sendMessageToFrontend(int callId,
                                                const hybrid::jschar_t *message,
                                                size_t size) {
        if (m_jthis) {
            JNIEnv *env = AttachCurrentThread();
            jstring jstr_msg = toJavaString(env, message, size);
            env->CallVoidMethod(m_jthis, _sendResponse, m_sessionId, 0, jstr_msg);
            env->DeleteLocalRef(jstr_msg);
        }
    }

    HybridInspector::HybridInspector(int sessionId)
            : m_jthis(0),
              m_v8Runtime(NULL),
              m_sessionId(sessionId),
              js_env_(nullptr, hybrid::JSEnv::Release) {}

    HybridInspector::~HybridInspector() {
        if (m_jthis != 0) {
            AttachCurrentThread()->DeleteGlobalRef(m_jthis);
        }
    }

    void HybridInspector::initialzeV8Context(int is_jscontext_recreated) {
        js_env_.reset(hybrid::JSEnv::GetInstance(m_v8Runtime));
        inspector_client_.reset(
                new InspectorClientImpl(this, js_env_.get(), is_jscontext_recreated));
    }

    void HybridInspector::clearV8Context() {
        inspector_client_.reset(nullptr);
        js_env_.reset(nullptr);
    }

    jstring HybridInspector::executeJsCode(JNIEnv *env, jstring jsCode) {
        if (!js_env_) {
            return nullptr;
        }

        jboolean is_copy = false;
        const hybrid::jschar_t *str_code = reinterpret_cast<const hybrid::jschar_t *>(
                env->GetStringChars(jsCode, &is_copy));
        size_t code_size = env->GetStringLength(jsCode);
        size_t result_size = 0;

        hybrid::JSValue result;
        bool bret = js_env_->ExecuteScript(str_code, code_size, &result, nullptr, 0);

        if (is_copy) {
            env->ReleaseStringChars(jsCode, reinterpret_cast<const jchar *>(str_code));
        }

        if (!bret) {
            showException();
            return nullptr;
        }

        if (!result.IsString()) {
            ALOGE(TAG, "Inspector want get a string result");
            return nullptr;
        }

        jstring java_result = nullptr;

        if (result.IsUTF8String()) {
            java_result = env->NewStringUTF(result.UTF8Str());
        } else {
            java_result = env->NewString(
                    reinterpret_cast<const jchar *>(result.UTF16Str()), result.Length());
        }
        return java_result;
    }

    void HybridInspector::showException() {
        if (js_env_ && js_env_->HasException()) {
            hybrid::JSException exception = js_env_->GetException();

            ALOGE(TAG, "JS Exception: %s", exception.message);
        }
    }

    void HybridInspector::nativeHandleMessage(JNIEnv *env, jobject, jlong ptr,
                                              jint sessionId, jstring message) {
        HybridInspector *self = From(ptr);

        if (!self) {
            return;
        }

        jboolean is_copy = false;
        const jchar *message_ptr = env->GetStringChars(message, &is_copy);
        self->handleMessage(sessionId,
                            reinterpret_cast<const hybrid::jschar_t *>(message_ptr),
                            env->GetStringLength(message));
        if (is_copy) {
            env->ReleaseStringChars(message, message_ptr);
        }
    }

    jlong HybridInspector::initNative(JNIEnv *env, jobject thiz,
                                      jboolean /* autoEnable */, jint sessionId) {
        HybridInspector *inspector_instance = new HybridInspector(sessionId);
        inspector_instance->m_jthis = env->NewGlobalRef(thiz);

        return reinterpret_cast<jlong>(inspector_instance);
    }

    void HybridInspector::nativeSetV8Context(JNIEnv *env, jobject, jlong ptr,
                                             jobject v8,
                                             jint is_jscontext_recreated) {
        HybridInspector *self = From(ptr);

        if (!self) {
            return;
        }

        self->m_v8Runtime =
                reinterpret_cast<hybrid::J2V8Handle>(env->GetLongField(v8, _v8NativePtr));

        self->initialzeV8Context((int) is_jscontext_recreated);
    }

    void HybridInspector::nativeDisposeV8Context(JNIEnv *env, jobject, jlong ptr) {
        HybridInspector *self = From(ptr);

        if (!self) {
            return;
        }

        self->clearV8Context();
    }

    void HybridInspector::nativeBeginLoadJsCode(JNIEnv *env, jobject, jstring uri,
                                                jstring /*content*/) {}

    void HybridInspector::nativeEndLoadJsCode(JNIEnv * /*env*/, jobject,
                                              jstring /*uri*/) {}

    void HybridInspector::nativeDestroy(JNIEnv *env, jobject, jlong ptr) {
        HybridInspector *self = From(ptr);

        if (!self) {
            return;
        }
        delete self;
    }

    jstring HybridInspector::nativeExecuteJsCode(JNIEnv *env, jobject, jlong ptr,
                                                 jstring jsCode) {
        HybridInspector *self = From(ptr);
        if (!self) {
            return NULL;
        }
        return self->executeJsCode(env, jsCode);
    }

    jstring HybridInspector::nativeHandleConsoleMessage(JNIEnv *env, jobject,
                                                        jlong ptr,
                                                        jobject v8Array) {
        HybridInspector *self = From(ptr);
        if (!self) {
            return nullptr;
        }
        // donothing, remove me
        return nullptr;
    }

    void HybridInspector::nativeFrontendReload(JNIEnv *env, jobject, jlong ptr) {
        HybridInspector *self = From(ptr);

        if (!self) {
            return;
        }
        return self->onFrontendReload();
    }

    JNINativeMethod HybridInspector::_methods[] = {
            {"initNative",             "(ZI)J", (void *) HybridInspector::initNative},
            {"nativeHandleMessage",    "(JILjava/lang/String;)V",
                                                (void *) HybridInspector::nativeHandleMessage},
            {"nativeSetV8Context",     "(JLcom/eclipsesource/v8/V8;I)V",
                                                (void *) HybridInspector::nativeSetV8Context},
            {"nativeDisposeV8Context", "(J)V",
                                                (void *) HybridInspector::nativeDisposeV8Context},
            {"nativeBeginLoadJsCode",  "(Ljava/lang/String;Ljava/lang/String;)V",
                                                (void *) HybridInspector::nativeBeginLoadJsCode},  // TODO: DELETE.
            {"nativeEndLoadJsCode",    "(Ljava/lang/String;)V",
                                                (void *) HybridInspector::nativeEndLoadJsCode},  // TODO: DELETE.
            {"nativeDestroy",          "(J)V",  (void *) HybridInspector::nativeDestroy},
            {"nativeExecuteJsCode",    "(JLjava/lang/String;)Ljava/lang/String;",
                                                (void *) HybridInspector::nativeExecuteJsCode},
            {"nativeFrontendReload",   "(J)V",
                                                (void *) HybridInspector::nativeFrontendReload}};

    int HybridInspector::Initialize(JNIEnv *env) {
        jclass clazz = env->FindClass(V8InspectorClassName);
        _class = (jclass) env->NewGlobalRef(clazz);
        _sendResponse =
                env->GetMethodID(clazz, "sendResponse", "(IILjava/lang/String;)V");
        _sendNotification =
                env->GetMethodID(clazz, "sendNotification", "(IILjava/lang/String;)V");
        _runMessageLoopOnPause =
                env->GetMethodID(clazz, "runMessageLoopOnPause", "(I)V");
        _quitMessageLoopOnPause =
                env->GetMethodID(clazz, "quitMessageLoopOnPause", "()V");

        if (env->RegisterNatives(clazz, _methods,
                                 sizeof(_methods) / sizeof(_methods[0]))) {
            return JNI_FALSE;
        }

        // get v8
        jclass v8class = env->FindClass("com/eclipsesource/v8/V8");
        _v8NativePtr = env->GetFieldID(v8class, "v8RuntimePtr", "J");

        // get v8Array
        jclass v8ValueClass = env->FindClass("com/eclipsesource/v8/V8Value");
        _v8ValueHandlePtr = env->GetFieldID(v8ValueClass, "objectHandle", "J");
        return JNI_TRUE;
    }

    jclass HybridInspector::_class;
    jmethodID HybridInspector::_sendResponse;
    jmethodID HybridInspector::_sendNotification;
    jmethodID HybridInspector::_runMessageLoopOnPause;
    jmethodID HybridInspector::_quitMessageLoopOnPause;
    jfieldID HybridInspector::_v8NativePtr;
    jfieldID HybridInspector::_v8ValueHandlePtr;

}  // namespace inspector

/**
 * 我在loader中加载了reload命令，该命令是为了调试首页。
 * 因为inspect页面与loader连接是只能发生在加载成功后，
 * (当然，也可以在loader中增加等待inspect连接的功能，但是重建加载首页是必要的)
 * 而且，只有首页的内容被v8加载后才能显示在inspect/source页面中，
 * 所以，首页第一次连接时是不能被调试的，所以要增加一个reload功能。
 *
 * reload需要销毁原来的v8,
 * 再重新创建一个。这样，我们就必须重新创建所有的v8后端的agent，
 * 被载入到v8内的js源码也会被重新加载一遍。
 * 在v8中每个js代码都有一个scriptId，这是v8内部生成，一旦加载源码就会确定，不会发生变化。
 * 这个scriptId会被传递给inspect，inspect用这个scriptId和v8引擎进行沟通。
 */

//////////////////////////////////////////
jint JNI_OnLoad(JavaVM *vm, void *) {
    JNIEnv *env = NULL;
    jint result = -1;

    g_jvm = vm;

    if (vm->GetEnv((void **) &env, JNI_VERSION_1_4) != JNI_OK) {
        return result;
    }

    if (inspector::HybridInspector::Initialize(env) != JNI_TRUE) {
        ALOGE("Inspector", "initialize failed");
        return result;
    }

    NativeReflectInit(env);

    return JNI_VERSION_1_4;
}

///////////////////////////////////////////////////
