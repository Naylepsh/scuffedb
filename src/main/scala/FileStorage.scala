import java.nio.charset.StandardCharsets
import java.nio.file.*
import java.time.Instant

import scala.collection.mutable.TreeMap
import scala.jdk.CollectionConverters.*
import scala.util.control.NoStackTrace
import scala.util.chaining.*

case object NotADirectory extends NoStackTrace
type NotADirectory = NotADirectory.type

class FileStorage(pathToDiskStorage: Path):
  import Line.*

  def add(memTable: TreeMap[String, String]): Unit =
    val fileId  = Instant.now().toString
    val content = memTable.map(Line.make).mkString("\n")
    add(fileId, content)

  def add(lines: List[Line]): Unit =
    val fileId  = Instant.now().toString
    val content = lines.mkString("\n")
    add(fileId, content)

  private def add(fileId: String, content: String): Unit =
    Files.write(
      pathToDiskStorage.resolve(fileId),
      content.getBytes(StandardCharsets.UTF_8),
      StandardOpenOption.CREATE,
      StandardOpenOption.TRUNCATE_EXISTING
    )

  def find(key: String): Option[String] =
    val k     = key.toString
    val files = Files.list(pathToDiskStorage).iterator.asScala
    files
      .foldLeft(None): (_, path) =>
        Files
          .lines(path)
          .iterator
          .asScala
          .foldLeft(None: Option[String]):
            case (_, line) if line.startsWith(k) => Some(line.value)
            case (none, _)                       => none
      .map(_.value)

  def mergeAndCompact(): Unit =
    val files = Files
      .list(pathToDiskStorage)
      .iterator
      .asScala
      .toList

    if files.length > 1 then
      files
        .map: file =>
          Files.readAllLines(file).asScala.toList
        .toList
        .pipe(FileStorage.merge)
        .pipe(add)

      files.foreach: file =>
        Files.delete(file)

object FileStorage:
  import Line.*

  def apply(pathToDiskStorage: Path): Either[NotADirectory, FileStorage] =
    Either.cond(
      Files.isDirectory(pathToDiskStorage),
      new FileStorage(pathToDiskStorage),
      NotADirectory
    )

  def merge(contents: List[List[Line]]): List[Line] =
    // Assumes that files are ordered from oldest to latest
    contents
      .map: fileLines =>
        fileLines.map(_.entry)
      .pipe(merge(_, List.empty))
      .map(make)

  @scala.annotation.tailrec
  def merge(
      content: List[List[(String, String)]],
      acc: List[(String, String)]
  ): List[(String, String)] =
    content match
      case Nil => acc.reverse
      case _ =>
        content.filter(_.nonEmpty) match
          case Nil => merge(Nil, acc)
          case nonEmpties =>
            val smallest = nonEmpties.map(_.head).minBy(_._1)

            val xs = nonEmpties.map:
              case head :: tail if head._1 == smallest._1 => tail
              case other                                  => other
            merge(xs, smallest :: acc)
