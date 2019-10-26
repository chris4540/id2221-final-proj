package se.kth.id2221.chrisandmikolaj.finalproject.util

case class InfluxConfig(host: String, port: Int, username: String, password: String)

object InfluxConfig {
  def fromEnv = InfluxConfig(
    host = sys.env.getOrElse("INFLUXDB_HOST", "influxdb"),
    port = sys.env.getOrElse("INFLUXDB_PORT", "8086").toInt,
    username = sys.env.getOrElse("INFLUXDB_USERNAME", "admin"),
    password = sys.env.getOrElse("INFLUXDB_PASSWORD", "admin")
  )
}