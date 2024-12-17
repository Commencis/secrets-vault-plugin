# Secrets Vault Plugin

This **Android-focused Gradle plugin** provides developers with a comprehensive tool to securely hide and obfuscate secrets within their Android projects.

The Secrets Vault Plugin employs multiple security-enhancing strategies, including:
- **Reversible XOR Operator:** Utilizing the reversible XOR operator to obfuscate secrets, thereby ensuring they are never exposed in plain text.
- **Obfuscated Storage in NDK Binary:** Safely storing the obfuscated secret in an NDK binary as a hexadecimal array, making it extremely difficult to identify or reconstruct from disassembly.
- **App Signature Check:** Performs an app signature check, which validates the authenticity of the application. This check prevents unauthorized access to the generated .so file, adding an additional layer of protection against tampering or unauthorized usage.
- **Different Source Set and Flavor Support:** Developers can differentiate and categorize their secrets based on unique source sets or flavors, such as 'google' or 'prod'. This feature provides an organized, efficient, and modular approach to handle secrets, especially for larger projects or apps with multiple distribution channels and variations.
- **Custom Encoding/Decoding Algorithm:** Providing an option for users to implement their own encoding/decoding algorithm, thereby introducing an additional layer of security.

> Please remember that no client-side security measures are invincible. As a rule of thumb, **storing secrets in a mobile app is not considered best practice**. However, when there's no other option, this method is our best recommendation for concealing them.

# 1) Getting Started

To use the Secrets Vault Plugin in your Android project, follow these steps:

### Step 1: Update the root `build.gradle[.kts]` file

In your root `build.gradle[.kts]` file, add the following plugin configuration:

```gradle
plugins {
  ...
  id "com.commencis.secretsvaultplugin" version "[write the latest version]" apply false
}
```

### Step 2: Apply the plugin in the module `build.gradle[.kts]` file

Apply the plugin in the module `build.gradle[.kts]` file:

```gradle
plugins {
  ...
  id 'com.commencis.secretsvaultplugin'
}
```

# 2) Keep Secrets in Your Project

To keep secrets in your project, you can add them to a JSON file located in the root folder of the module where you've applied the plugin.
Follow the format below:
```json
[
    { "key": "apiKey1", "value": "API_VALUE_1" },
    { "key": "apiKey2", "value": "API_VALUE_2" },
    ...
]
```

You can obfuscate and hide your secret keys in your project using the following command:
```shell
./gradlew keepSecrets
```

You can also configure the plugin by defining the secretsVault block in your module-level `build.gradle[.kts]` file:
```gradle
secretsVault {
    secretsFile = file("YourSecretsJsonFile.json") // Optional. secrets.json file in the module by default.
    packageName = "YourPackageName" // Optional. Uses the namespace of the module where the plugin is applied by default.
    appSignatures = ["YourAppSignatureForDebug", "YourAppSignatureForRelease"] // Optional. Empty by default.
    obfuscationKey = "YourObfuscationKey" // Optional. A randomly generated alphanumeric string of length 32 by default.
    sourceSetSecretsMappingFile = file("YourSourceSetMappingFile.json") // Optional. Used for multi-dimensional flavors mapping.
    makeInjectable = false // Optional. Defaults to 'false'. Makes the generated Kotlin class injectable.
    cmake { // Optional
        projectName = "YourCMakeProjectName" // Optional. Defaults to the module name.
        version = "YourCMakeVersion" // Optional. Default CMake version used otherwise.
    }
}
```

- The `secretsFile` parameter is optional and uses `secrets.json` file by default in the root folder of the module where you've applied the plugin. You can provide your own JSON file if desired.
- The `packageName` parameter is optional and uses the namespace in the module where the plugin applied by default. You can specify a different package name if needed.
- The `appSignatures` parameter is optional, but it is highly recommended to add an app signature check for better security.
  Use the `./gradlew signingReport` command to retrieve the MD5 hash of your debug and release signings. Add these values to the `appSignatures` option in the `secretsVault` configuration in the `build.gradle[.kts]` file.
- The `obfuscationKey` parameter is optional and defaults to a randomly generated alphanumeric string of length 32.
- The `sourceSetSecretsMappingFile` parameter is optional and is used to map specific secrets files to distinct source sets, beneficial for multi-dimensional flavors.
- The `makeInjectable` parameter is optional and determines if the generated Kotlin class should include the @Inject annotation for its constructor. By default, it is set to `false`.
- The `cmake` block is optional and contains additional optional parameters:
  - The `projectName` parameter is optional and uses the module's name where the plugin is applied by default. You can specify a different project name if needed.
  - The `version` parameter is optional. If not provided, a default CMake version will be used.

# 3) Get Your Secret Key in Your App
To enable the compilation of C++ files, add these lines in the Module level `build.gradle[.kts]` :
```gradle
android {
    ...

    // Configure NDK build for C++ files
    externalNativeBuild {
        cmake {
            path "src/main/cpp/CMakeLists.txt"
        }
    }
}
```

## Access inside Kotlin

Access your secret key by calling :
```kotlin
val key = MainSecrets().getYourSecretKeyName()
```

## Access inside Java

The names of the methods in the generated files (`MainSecrets.kt` & `Secrets.kt`) are replaced with dummy strings such as `a0` on the `JVM` side for extra security. This makes accessing these methods inside `Java` classes a bit tricky since those names are not "readable" and are dependent on the order in `secrets.json` file.

As as solution, a helper/wrapper `Kotlin` class can be created instead of accessing these methods directly inside a `Java` class:

`MySecretsHelper.kt`:
```kotlin
internal class MySecretsHelper {
  val secrets = MainSecrets() // Suppose contains "external fun getYourSecretKeyName(): String" with "@JvmName("a0")" annotation.
  val mySecret: String get() = secrets.getYourSecretKeyName()
}
```

Inside your `Java` class, instead of doing:

```java
class MySecretsConsumer {
  final MainSecrets secrets = new MainSecrets();

  void someMethod() {
    final String key = secrets.a0();
    ...
  }
}
```

You can do:
```java
class MySecretsConsumer {
  final MySecretsHelper secretsHelper = new MySecretsHelper();

  void someMethod() {
    final String key = secretsHelper.getMySecret();
    ...
  }
}
```

# 4) Flavor-specific Secrets (Optional)
If you are working on multi-flavor projects and have flavor-specific secrets, you need to pass arguments to CMake in your `build.gradle[.kts]` file. Follow the steps below:

```gradle
android {
    ...
    
    productFlavors {
            flavorName {
                ...
                externalNativeBuild {
                    cmake {
                        // Pass arguments to CMake
                        arguments "-DsourceSet=flavorName"
                    }
                }
            }
        }
    }
}
```
Once you have set up the flavor-specific configuration, you can include the secrets in your `secrets.json` file by specifying the source set or flavor name using the `sourceSet` parameter. If a secret is independent of source sets, you can omit the `sourceSet` parameter, and it will be accessible from all source sets.

Here is an example of the `secrets.json` file structure:

```json
[
    { "key": "apiKey1", "value": "API_VALUE_1_DEVELOPMENT", "sourceSet": "dev" },
    { "key": "apiKey1", "value": "API_VALUE_1_PRODUCTION", "sourceSet": "prod" },
    { "key": "apiKey2", "value": "API_VALUE_2_DEVELOPMENT", "sourceSet": "dev" },
    { "key": "apiKey2", "value": "API_VALUE_2_PRODUCTION", "sourceSet": "prod" },
    { "key": "apiKey4", "value": "API_VALUE_4_GENERAL" }
]
```

To access the flavor-specific secrets within your app, you can call the corresponding methods from the `Secrets()` class. For example, use `Secrets().getApiKey1()` to retrieve the value of `apiKey1` that changes based on the current flavor.

For global secrets that are accessible from all source sets and flavours, you can use the methods from the `MainSecrets()` class, such as `MainSecrets().getApiKey4()`.

> Note: Make sure that you update the `secrets.json` file with the relevant secrets for each source set in your Android project.

# 5) Multi-Dimensional Flavors (Advanced - Optional)
In complex projects with multiple build variants or distribution channels, managing secrets across these dimensions can be a challenge. The Secrets Vault Plugin simplifies this by introducing support for Multi-Dimensional Flavors.

This feature allows developers to map specific secrets to distinct source sets, ensuring a streamlined approach to managing secrets based on various build configurations.

## Format and Usage:
To make use of Multi-Dimensional Flavors, specify a mapping using the following JSON format:

Here is an example of the `sourceSetMapping.json` file structure:

```json
[
    {
        "secretsFileName" : "MobileServicesSecrets",
        "cmakeArgument": "mobileServices",
        "sourceSets" : [ "google", "huawei" ]
    },
    {
        "secretsFileName" : "Secrets",
        "cmakeArgument": "version",
        "sourceSets" : [ "dev", "qa" ]
    }
]
```

### Attributes Explained:

`secretsFileName`: Specifies the name of the generated secrets file, ensuring each set of secrets is distinctly accessible.

`cmakeArgument`: Represents the specific argument name that will be passed to CMake during the native build process.

`sourceSets`: A list of source sets that will use a particular set of secrets, ensuring precise mapping and access.

## Configuration:

Firstly, provide the mapping file to the Secrets Vault Plugin in secretsVault block:

```gradle
secretsVault {
    ...
    sourceSetSecretsMappingFile = file("sourceSetMapping.json") // Point to the JSON file that contains your mapping configuration.
}
```

To correctly configure the multi-dimensional flavors, ensure you update the argument in your build configuration to match the `cmakeArgument` specified in the JSON:

```gradle
android {
    ...
    
    productFlavors {
        flavorName {
            ...
            externalNativeBuild {
                cmake {
                    // Pass arguments to CMake. Match the 'cmakeArgument' from your JSON configuration.
                    arguments "-Dversion=flavorName" // For example, for 'cmakeArgument': 'version'
                }
            }
        }
    }
}
```
Remember to replace version in -Dversion=flavorName with the appropriate cmakeArgument from your JSON. The flavorName should correspond to the specific product flavor you're building for.

# 6) Enhance Your Secrets' Security (Optional)
To enhance the security of your secrets, you can create a custom encoding/decoding algorithm. The secrets will be stored in C++ and further secured by applying your custom encoding algorithm. Additionally, the decoding algorithm will be compiled, making it more challenging for an attacker to reverse-engineer and obtain your keys.

Encode all values in your `secrets.json` file.

Then, implement the decoding logic, add your custom decoding code to the `customDecode` method in `secrets.cpp`.
```cpp
void customDecode(char *str) {
    // Implement your custom logic here.
}
```

The `customDecode` method is automatically invoked when calling:
```kotlin
Secrets().getYourSecretKeyName()
```

# Credits
This project is inspired by [Hidden Secrets Gradle Plugin](https://github.com/klaxit/hidden-secrets-gradle-plugin) developed by Klaxit. It shares some common code with the Hidden Secrets Gradle Plugin, which served as a valuable reference during the development of this project.

# License
This project is licensed under the MIT License. You are free to modify, distribute, and use the code in your projects. Please refer to the [LICENSE](https://github.com/Commencis/secrets-vault-plugin/blob/main/LICENSE) file for more details.
