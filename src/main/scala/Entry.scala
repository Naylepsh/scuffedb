enum Status:
  case Active, Inactive

case class Entry(key: String, value: String, status: Status)
object Entry:
  def makeActive(key: String, value: String): Entry =
    Entry(key, value, Status.Active)

  def makeInactive(key: String, value: String): Entry =
    Entry(key, value, Status.Inactive)
