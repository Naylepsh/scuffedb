package scuffedb

import scala.collection.mutable.TreeMap

import bloomfilter.mutable.BloomFilter
import cats.effect.IO
import cats.effect.std.Mutex
import cats.syntax.all.*

import domain.*

trait StorageEngine:
  def add(key: String, value: String): IO[Unit]
  def find(key: String): IO[Option[String]]
  def delete(key: String): IO[Unit]
  def flush(): IO[Unit]
  def mergeAndCompact(): IO[Unit]
  def restore(): IO[Unit]

class SimpleStorageEngine(
    /* This is a very naive metric.
     * It would be better to have a maxSize not for items, but for their total size.
     * Currently nothing stops the user from inserting a couple of large items that together don't fit in memory.
     */
    maxItemCount: Long,
    fileStorage: FileStorage,
    appendLog: AppendLog
) extends StorageEngine:
  val memTable = TreeMap.empty[String, Entry]
  val bloomFilter = bloomfilter.mutable
    .BloomFilter[String](numberOfItems = 1_000_000, falsePositiveRate = 0.1)

  def add(key: String, value: String): IO[Unit] =
    add(Entry.makeActive(key, value)) *> IO.delay(bloomFilter.add(key))

  def find(key: String): IO[Option[String]] = IO.delay:
    memTable
      .get(key)
      .fold(
        if bloomFilter.mightContain(key) then fileStorage.find(key) else None
      ): entry =>
        Option.when(entry.mark != Mark.Tombstone)(entry.value)

  def delete(key: String): IO[Unit] =
    add(Entry.makeTombstone(key))

  private def add(entry: Entry): IO[Unit] =
    appendLog.add(entry)
    memTable += (entry.key -> entry)
    if memTable.size >= maxItemCount then flush() else IO.unit

  def flush(): IO[Unit] = IO.delay:
    fileStorage.add(memTable.values.toList)
    memTable.clear()
    appendLog.clear()

  def mergeAndCompact(): IO[Unit] = IO.delay:
    val keys = fileStorage.mergeAndCompact()
    bloomFilter.dispose()
    keys.foreach(bloomFilter.add)

  def restore(): IO[Unit] =
    IO.raiseUnless(memTable.isEmpty)(
      new RuntimeException("Cannot restore state when it's already initialized")
    ).flatTap: _ =>
      appendLog.read().traverse(entry => add(entry.key, entry.value))

class MultithreadedStorageEngine(engine: SimpleStorageEngine, mutex: Mutex[IO])
    extends StorageEngine:

  override def add(key: String, value: String): IO[Unit] =
    mutex.lock.surround:
      engine.add(key, value)

  override def find(key: String): IO[Option[String]] =
    engine.find(key)

  override def delete(key: String): IO[Unit] =
    engine.delete(key)

  override def flush(): IO[Unit] =
    mutex.lock.surround:
      engine.flush()

  override def mergeAndCompact(): IO[Unit] =
    mutex.lock.surround:
      engine.mergeAndCompact()

  override def restore(): IO[Unit] =
    mutex.lock.surround:
      engine.restore()

object MultithreadedStorageEngine:
  def apply(engine: SimpleStorageEngine): IO[MultithreadedStorageEngine] =
    Mutex[IO].map(mutex => new MultithreadedStorageEngine(engine, mutex))
