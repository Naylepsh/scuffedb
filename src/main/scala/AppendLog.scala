import java.nio.charset.StandardCharsets
import java.nio.file.*

import scala.jdk.CollectionConverters.*

import cats.syntax.all.*

class AppendLog(pathToLog: Path)(using codec: Codec[Entry]):
  def add(entry: Entry): Unit =
    Files.write(
      pathToLog,
      s"${codec.encode(entry)}\n"
        .getBytes(StandardCharsets.UTF_8),
      StandardOpenOption.CREATE,
      StandardOpenOption.APPEND
    )

  def read(): List[Entry] =
    if Files.exists(pathToLog) then
      Files
        .readAllLines(pathToLog)
        .iterator()
        .asScala
        .toList
        .traverse(codec.decode)
        .fold(throw _, identity)
    else List.empty

  def clear(): Unit =
    Files.deleteIfExists(pathToLog)
