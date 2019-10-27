package se.kth.id2221.chrisandmikolaj.finalproject.data

import com.paulgoldbaum.influxdbclient.Point
import se.kth.id2221.chrisandmikolaj.finalproject.util.ToPoint

case class ActiveUser(username: String, activityCount: Int) {
  def +(other: ActiveUser): ActiveUser = copy(activityCount = this.activityCount + other.activityCount)
}

object ActiveUser {
  implicit val activeUserToPoint: ToPoint[ActiveUser] = {
    case (ActiveUser(username, activityCount), meas) =>
      Point(meas)
        .addTag("username", username)
        .addField("activity_count", activityCount)
  }
}