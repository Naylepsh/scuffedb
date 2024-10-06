import java.nio.charset.StandardCharsets
import java.nio.file.*

import scala.jdk.CollectionConverters.*

class AppendLog(pathToLog: Path):
  def add(key: String, value: String): Unit =
    Files.write(
      pathToLog,
      s"${Line.make(key, value)}\n".getBytes(StandardCharsets.UTF_8),
      StandardOpenOption.CREATE,
      StandardOpenOption.APPEND
    )

  def read(): List[(String, String)] =
    if Files.exists(pathToLog) then
      Files.readAllLines(pathToLog).iterator().asScala.toList.map(Line.entry)
    else List.empty

  def clear(): Unit =
    Files.deleteIfExists(pathToLog)
