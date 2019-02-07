package com.awolity.secftp.ssh

data class ConnectionData(val host:String,
                          val portNumber : Int,
                          val username: String,
                          val password: String?,
                          val privateKeyFilePath: String?)