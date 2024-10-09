import java.nio.charset.StandardCharsets
import java.nio.file.*
import java.time.Instant

import scala.collection.mutable.TreeMap
import scala.jdk.CollectionConverters.*
import scala.util.control.NoStackTrace

import cats.syntax.all.*

case object NotADirectory extends NoStackTrace
type NotADirectory = NotADirectory.type

class FileStorage(pathToDiskStorage: Path)(using codec: Codec[Entry]):
  def add(memTable: TreeMap[String, String]): Unit =
    val fileId = Instant.now().toString
    val content =
      memTable.map(Entry.makeActive.tupled.andThen(codec.encode)).mkString("\n")
    add(fileId, content)

  def add(entries: List[Entry]): Unit =
    val fileId  = Instant.now().toString
    val content = entries.map(codec.encode).mkString("\n")
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
          .toList
          .traverse(codec.decode)
          .fold(throw _, identity)
          .foldLeft(None: Option[String]):
            case (_, entry) if entry.key.eqv(key) => Some(entry.value)
            case (none, _)                        => none

  def mergeAndCompact(): Unit =
    val files = Files
      .list(pathToDiskStorage)
      .iterator
      .asScala
      .toList

    if files.length > 1 then
      files
        .map(Files.readAllLines(_).asScala.toList)
        .toList
        .traverse(_.traverse(codec.decode))
        .map(FileStorage.merge.andThen(add))
        .fold(throw _, _ => files.foreach(Files.delete))

  def clear(): Unit =
    Files.list(pathToDiskStorage).forEach(Files.deleteIfExists)

object FileStorage:
  def apply(pathToDiskStorage: Path): Either[NotADirectory, FileStorage] =
    Either.cond(
      Files.isDirectory(pathToDiskStorage),
      new FileStorage(pathToDiskStorage),
      NotADirectory
    )

  def merge(contents: List[List[Entry]]): List[Entry] =
    merge(contents, List.empty)

  @scala.annotation.tailrec
  private def merge(content: List[List[Entry]], acc: List[Entry]): List[Entry] =
    // Assumes that files are ordered from oldest to latest
    content match
      case Nil => acc.reverse
      case _ =>
        content.filter(_.nonEmpty) match
          case Nil => merge(Nil, acc)
          case nonEmpties =>
            val smallest = nonEmpties.map(_.head).minBy(_.key)

            val leftoverContent = nonEmpties.map:
              case head :: tail if head.key == smallest.key => tail
              case other                                    => other
            val newAcc = smallest.status match
              case Status.Active   => smallest :: acc
              case Status.Inactive => acc
            merge(leftoverContent, newAcc)
