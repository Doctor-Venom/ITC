package com.venom.itc.utils

class ChatMessage(var msg: String, val msg_id: String,  val sender_usr_id: String, val receiver_usr_id: String, val timestamp: Long ){
    constructor(): this("","","","",-1)
}