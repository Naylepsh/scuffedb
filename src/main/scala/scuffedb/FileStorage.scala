package scuffedb

import java.nio.charset.StandardCharsets
import java.nio.file.*
import java.time.Instant

import scala.collection.mutable.TreeMap
import scala.jdk.CollectionConverters.*
import scala.util.chaining.*
import scala.util.control.NoStackTrace

import cats.syntax.all.*

import codecs.*
import domain.*

case object NotADirectory extends NoStackTrace
type NotADirectory = NotADirectory.type

class FileStorage(pathToDiskStorage: Path)(using codec: Codec[Entry]):
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
    findInFiles(
      key.toString,
      /*
       * The files are implicitly ordered from oldest to latest (due to their naming).
       * Need to reverse to convert to latest-to-oldest
       */
      Files.list(pathToDiskStorage).iterator.asScala.toList.reverse
    )

  /** Assumes that paths are ordered from latest to oldest
    */
  @scala.annotation.tailrec
  private def findInFiles(key: String, paths: List[Path]): Option[String] =
    paths match
      case Nil          => None
      case path :: tail =>
        // Reverse to make it so that latest entries are at the top
        parse(path).reverse.pipe(findInContent(key, _)) match
          case None                               => findInFiles(key, tail)
          case Some(Entry(_, _, Mark.Tombstone))  => None
          case Some(Entry(_, value, Mark.Active)) => Some(value)

  private def parse(path: Path): List[Entry] =
    Files
      .lines(path)
      .iterator
      .asScala
      .toList
      .traverse(codec.decode)
      .fold(throw _, identity)

  @scala.annotation.tailrec
  private def findInContent(key: String, entries: List[Entry]): Option[Entry] =
    entries match
      case Nil                            => None
      case entry :: _ if entry.key == key => Some(entry)
      case _ :: tail                      => findInContent(key, tail)

  def mergeAndCompact(): Unit =
    val files = Files
      .list(pathToDiskStorage)
      .iterator
      .asScala
      .toList
      .reverse

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
  def apply(
      pathToDiskStorage: Path
  )(using Codec[Entry]): Either[NotADirectory, FileStorage] =
    Either.cond(
      Files.isDirectory(pathToDiskStorage),
      new FileStorage(pathToDiskStorage),
      NotADirectory
    )

  /* Assumes that files are ordered from latest to oldest
   */
  def merge(contents: List[List[Entry]]): List[Entry] =
    merge(contents, List.empty)

  /* Assumes that files are ordered from latest to oldest
   */
  @scala.annotation.tailrec
  private def merge(content: List[List[Entry]], acc: List[Entry]): List[Entry] =
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
            val newAcc = smallest.mark match
              case Mark.Active    => smallest :: acc
              case Mark.Tombstone => acc
            merge(leftoverContent, newAcc)
