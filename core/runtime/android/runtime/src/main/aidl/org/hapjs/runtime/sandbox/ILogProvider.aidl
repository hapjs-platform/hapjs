/*
 * Copyright (C) 2023, hapjs.org. All rights reserved.
 */
package org.hapjs.runtime.sandbox;

interface ILogProvider {
    void logCountEvent(String appPackage, String category, String key);

    void logCountEventWithParams(String appPackage, String category, String key, in Map params);

    void logCalculateEvent(String appPackage, String category, String key, long value);

    void logCalculateEventWithParams(String appPackage, String category, String key, long value, in Map params);

    void logNumericPropertyEvent(String appPackage, String category, String key, long value);

    void logNumericPropertyEventWithParams(String appPackage, String category, String key, long value, in Map params);

    void logStringPropertyEvent(String appPackage, String category, String key, String value);

    void logStringPropertyEventWithParams(String appPackage, String category, String key, String value, in Map params);
}