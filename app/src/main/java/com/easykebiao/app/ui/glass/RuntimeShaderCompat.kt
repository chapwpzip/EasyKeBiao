package com.easykebiao.app.ui.glass

import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast

@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.TIRAMISU)
fun supportsRuntimeShaderEffect(): Boolean {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
}
