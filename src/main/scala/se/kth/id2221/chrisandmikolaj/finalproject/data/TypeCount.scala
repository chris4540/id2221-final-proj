package se.kth.id2221.chrisandmikolaj.finalproject.data

import com.paulgoldbaum.influxdbclient.Point
import se.kth.id2221.chrisandmikolaj.finalproject.util.ToPoint

case class TypeCount(typ: String, count: Int) {
  def +(other: TypeCount): TypeCount = copy(count = this.count + other.count)
}

object TypeCount {
  implicit val typeCountToPoint: ToPoint[TypeCount] = {
    case (TypeCount(typ, count), meas) =>
      Point(meas)
        .addTag("type", typ)
        .addField("count", count)
  }
}
