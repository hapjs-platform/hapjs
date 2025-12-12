/*
 * Copyright (C) 2023, hapjs.org. All rights reserved.
 */
package org.hapjs.runtime.sandbox;

public interface SandboxIpcMethods {
    String HEART_BEAT = "heartBeat";
    String BLOCK = "block";
    String UNBLOCK = "unblock";
    String ON_ATTACH = "onAttach";
    String NOTIFY_CONFIGURATION_CHANGED = "notifyConfigurationChanged";
    String UPDATE_LOCALE = "updateLocale";
    String REGISTER_BUNDLE_CHUNKS = "registerBundleChunks";
    String CREATE_APPLICATION = "createApplication";
    String REQUEST_APPLICATION = "requestApplication";
    String ON_SHOW_APPLICATION = "onShowApplication";
    String ON_HIDE_APPLICATION = "onHideApplication";
    String BACK_PRESS_PAGE = "backPressPage";
    String MENU_BUTTON_PRESS_PAGE = "menuButtonPressPage";
    String KEY_PRESS_PAGE = "keyPressPage";
    String MENU_PRESS_PAGE = "menuPressPage";
    String ORIENTATION_CHANGE_PAGE = "orientationChangePage";
    String EXECUTE_VOID_SCRIPT = "executeVoidScript";
    String EXECUTE_SCRIPT = "executeScript";
    String EXECUTE_VOID_FUNCTION = "executeVoidFunction";
    String EXECUTE_OBJECT_SCRIPT_AND_STRINGIFY = "executeObjectScriptAndStringify";
    String CREATE_PAGE = "createPage";
    String RECREATE_PAGE = "recreatePage";
    String REFRESH_PAGE = "refreshPage";
    String NOTIFY_PAGE_NOT_FOUND = "notifyPageNotFound";
    String DESTROY_PAGE = "destroyPage";
    String DESTROY_APPLICATION = "destroyApplication";
    String FIRE_EVENT = "fireEvent";
    String FIRE_KEY_EVENT = "fireKeyEvent";
    String FIRE_CALLBACK = "fireCallback";
    String ON_FOLD_CARD = "onFoldCard";
    String REACH_PAGE_TOP = "reachPageTop";
    String REACH_PAGE_BOTTOM = "reachPageBottom";
    String PAGE_SCROLL = "pageScroll";
    String REGISTER_COMPONENTS = "registerComponents";
    String TERMINATE_EXECUTION = "terminateExecution";
    String SHUTDOWN = "shutdown";
    String INSPECTOR_HANDLE_MESSAGE = "inspectorHandleMessage";
    String INSPECTOR_INIT = "inspectorInit";
    String INSPECTOR_SET_V8_CONTEXT = "inspectorSetV8Context";
    String INSPECTOR_DISPOSE_V8_CONTEXT = "inspectorDisposeV8Context";
    String INSPECTOR_DESTROY = "inspectorDestroy";
    String INSPECTOR_BEGIN_LOAD_JS_CODE = "inspectorBeginLoadJsCode";
    String INSPECTOR_END_LOAD_JS_CODE = "inspectorEndLoadJsCode";
    String INSPECTOR_EXECUTE_JS_CODE = "inspectorExecuteJsCode";
    String INSPECTOR_FRONTEND_RELOAD = "inspectorFrontendReload";
    String ON_FRAME_CALLBACK = "onFrameCallback";

    String CALL_NATIVE = "callNative";
    String GET_VIEW_ID = "getViewId";
    String READ_DEBUG_ASSET = "readDebugAsset";
    String ON_KEY_EVENT_CALLBACK = "onKeyEventCallback";
    String INVOKE_FEATURE = "invokeFeature";
    String ROUTER_BACK = "routerBack";
    String ROUTER_PUSH = "routerPush";
    String ROUTER_CLEAR = "routerClear";
    String ROUTER_REPLACE = "routerReplace";
    String INSPECTOR_RESPONSE = "inspectorResponse";
    String INSPECTOR_SEND_NOTIFICATION = "sendNotification";
    String INSPECTOR_RUN_MESSAGE_LOOP_ON_PAUSE = "runMessageLoopOnPause";
    String INSPECTOR_QUIT_MESSAGE_LOOP_ON_PAUSE = "quitMessageLoopOnPause";
    String PROFILER_IS_ENABLED = "profilerIsEnabled";
    String PROFILER_RECORD = "profilerRecord";
    String PROFILER_SAVE_PROFILER_DATA = "profilerSaveProfilerData";
    String PROFILER_TIME_END = "profilerTimeEnd";
    String ON_V8_EXCEPTION = "onV8Exception";
    String REQUEST_ANIMATION_FRAME_NATIVE = "requestAnimationFrameNative";
}
