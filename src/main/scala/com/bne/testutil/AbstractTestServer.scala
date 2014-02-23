package com.bne.testutil

import java.lang.ProcessBuilder
import java.net.InetSocketAddress
import java.util.concurrent.Executors
import java.io.{BufferedWriter, FileWriter, PrintWriter, File, BufferedReader, InputStreamReader}
import com.twitter.util.RandomSocket
import com.twitter.util.FuturePool
import collection.JavaConversions._
import scala.util.Random

trait ExternalServer { self =>
  private[this] val rand = new Random
  private[this] var process: Option[Process] = None
  private[this] val forbiddenPorts = 6300.until(7300)
  var address: Option[InetSocketAddress] = None

  protected val serverPresentCmd:(String,String)
  protected val serverName:String

  private[this] def assertServerBinaryPresent() {
    val p = new ProcessBuilder(serverPresentCmd._1, serverPresentCmd._2).start()
    p.waitFor()
    val exitValue = p.exitValue()
    require(exitValue == 0 || exitValue == 1, serverName+" binary must be present.")
  }

  private[this] def findAddress() {
    var tries = 100
    while (address == None && tries >= 0) {
      address = Some(RandomSocket.nextAddress())
      if (forbiddenPorts.contains(address.get.getPort)) {
        address = None
        tries -= 1
        Thread.sleep(5)
      }
    }
    address.getOrElse { sys.error("Couldn't get an address for the external %s instance".format(serverName)) }
  }

  val futurePool = FuturePool(Executors.newCachedThreadPool)

  protected def cmd: Seq[String]
  protected def cmd_shutdown: Seq[String]

  def start() = {
    preStart()
    val builder = new ProcessBuilder(cmd.toList).redirectErrorStream(true)
    builder.directory(new File("/Users/lyrion/temp/"))
    print("%s starting at port %s...".format(serverName,address.get.getPort))
    process = Some(builder.start())
    futurePool{val br = new BufferedReader(new InputStreamReader(process.get.getInputStream()))
        var line = br.readLine()
        while(line!=null){
          println(line)
          line = br.readLine()
        }}
    Thread.sleep(1000)

    // Make sure the process is always killed eventually
    Runtime.getRuntime().addShutdownHook(new Thread {
      override def run() {
        self.stop()
      }
    });
    this
  }

  def stop() {
    val builder = new ProcessBuilder(cmd_shutdown.toList).redirectErrorStream(true)
    builder.directory(new File("/Users/lyrion/temp/"))
    print("%s shutting down at port %s...".format(serverName,address.get.getPort))
    val processDown = Some(builder.start())
    futurePool{val br = new BufferedReader(new InputStreamReader(processDown.get.getInputStream()))
        var line = br.readLine()
        while(line!=null){
          println(line)
          line = br.readLine()
        }}

    Thread.sleep(200)

    processDown.foreach { p =>
      p.destroy()
      p.waitFor()
    }

    Thread.sleep(200)

    process.foreach { p =>
      p.destroy()
      p.waitFor()
    }
    futurePool.executor.shutdown()
  }

  def restart() {
    stop()
    start()
  }

  def preStart() {
    assertServerBinaryPresent()
    findAddress()
  }
}
