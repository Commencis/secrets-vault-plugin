package YOUR_PACKAGE_NAME

import javax.inject.Inject

class SECRETS_CLASS_NAME @Inject constructor() {
    companion object {
        init {
            System.loadLibrary("secrets")
        }
    }
}
