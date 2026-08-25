package com.ehome.enpartesapp.ui.presupuesto

import android.net.Uri

data class PhotoItem(
    var photoUri: Uri? = null,
    var photoType: String = "",
    var photoKey: String = ""
)