type Line = String
object Line:
  extension (line: Line)
    def key: String     = entry._1
    def value: String = entry._2
    def entry: (String, String) =
      val content = line.split(":")
      (content.head, content.tail.head)

  def make(key: String, value: String): String = s"${key}:${value}"
