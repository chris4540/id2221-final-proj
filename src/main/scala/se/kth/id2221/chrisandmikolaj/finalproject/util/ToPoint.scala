package se.kth.id2221.chrisandmikolaj.finalproject.util

import com.paulgoldbaum.influxdbclient.Point

trait ToPoint[T] extends ((T, String) => Point) with Serializable

object ToPoint {
  def apply[T](implicit x: ToPoint[T]): ToPoint[T] = x

  implicit def intToPoint: ToPoint[Int] = (i, meas) => Point(meas).addField("value", i)

  implicit def doubleToPoint: ToPoint[Double] = (d, meas) => Point(meas).addField("value", d)

  implicit def stringToPoint: ToPoint[String] = (s, meas) => Point(meas).addField("value", s)

  implicit def booleanToPoint: ToPoint[Boolean] = (b, meas) => Point(meas).addField("value", b)

  implicit def pointToPoint: ToPoint[Point] = (p, meas) => p.copy(key = meas)
}