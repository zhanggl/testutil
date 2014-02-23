package com.bne.testutil

class MysqlTestServer extends ExternalServer {
	override val serverName = "Mysql Server"
	override def cmd = Seq("mysql.server","start","--port="+address.get.getPort)
	override val serverPresentCmd = ("mysql.server","--help")
	override def cmd_shutdown = Seq("mysql.server","stop","--port="+address.get.getPort)
}