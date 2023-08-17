package YOUR_PACKAGE_NAME

import javax.inject.Inject

class SECRETS_CLASS_NAME @Inject constructor() {

    EXTERNAL_METHODS_PLACEHOLDER

    companion object {
        init {
            System.loadLibrary("secrets")
        }
    }
}
