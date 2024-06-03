package com.youxel.library.model

import androidx.databinding.BaseObservable
import com.youxel.library.globalEnums.EnumAspectRatio
import com.youxel.library.globalEnums.EnumRepeatMode
import com.youxel.library.globalEnums.EnumPlayerSize
import com.youxel.library.globalEnums.EnumResizeMode

class PlayerControllerModel : BaseObservable() {

    var mute: Boolean = false
    var enumAspectRatio: EnumAspectRatio = EnumAspectRatio.ASPECT_16_9
    var enumRepeatMode: EnumRepeatMode = EnumRepeatMode.REPEAT_OFF
    var enumPlayerSize: EnumPlayerSize = EnumPlayerSize.EXACTLY
    var enumResizeMode: EnumResizeMode = EnumResizeMode.FILL

}