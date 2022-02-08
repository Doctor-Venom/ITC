package com.venom.itc.utils

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

//inherits from parcelable to enable passing user object via intents
@Parcelize//this is used to avoid parcelizing all attributes manually, it comes from experemental android extensions
class User(val uid: String, val pseudonym: String): Parcelable {
    constructor(): this("","")
}