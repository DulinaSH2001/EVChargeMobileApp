package com.example.evchargingapp.data.local

data class User(
    var nic: String = "",
    var name: String = "",
    var email: String = "",
    var phone: String = "",
    var password: String = ""
) {
    constructor() : this("", "", "", "", "")
}