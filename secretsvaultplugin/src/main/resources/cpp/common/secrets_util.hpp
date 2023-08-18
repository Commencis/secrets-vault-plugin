#ifndef SECRETS_UTILS_H
#define SECRETS_UTILS_H

#include "sha256.cpp"
#include <jni.h>

// Obfuscation key used to decode the obfuscated string
extern const char *obfuscatingStr;

/**
 * Decode the key using a custom logic.
 *
 * @param str The obfuscated key to be decoded
 * @return The decoded key
 */
char *customDecode(char *str);

/**
 * Decodes the obfuscated string using the obfuscation key.
 *
 * @param obfuscatedStr The obfuscated string to be decoded
 * @param obfuscatedStrSize The size of the obfuscated string
 * @return The decoded string
 */
char *decode(const char *obfuscatedStr, int obfuscatedStrSize);

/**
 * Retrieves the application context.
 *
 * @param pEnv A pointer to the JNI environment
 * @return The application context
 */
jobject getContext(JNIEnv *pEnv);

/**
 * Retrieves the app signature.
 *
 * @param pEnv A pointer to the JNI environment
 * @return The app signature
 */
const char *getSignature(JNIEnv *pEnv);

/**
 * Check if the app signature matches the expected signature.
 *
 * @param pEnv A pointer to the JNI environment
 * @param expectedSignature The expected signature
 * @return true if the app signature matches the expected signature, false otherwise
 */
bool checkAppSignatures(JNIEnv *pEnv, const char *expectedSignature);

/**
 * Get the original key.
 *
 * @param pEnv A pointer to the JNI environment
 * @return The original key
 */
const char *getOriginalKey(JNIEnv *pEnv);

#endif
