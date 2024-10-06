import scala.collection.mutable.TreeMap
import cats.effect.IO
import cats.effect.std.Mutex
import cats.syntax.all.*

trait StorageEngine:
  def add(key: String, value: String): IO[Unit]
  def find(key: String): IO[Option[String]]
  def flush(): IO[Unit]
  def mergeAndCompact(): IO[Unit]
  def restore(): IO[Unit]

class SimpleStorageEngine(
    maxSize: Long,
    fileStorage: FileStorage,
    appendLog: AppendLog
) extends StorageEngine:
  val memTable = TreeMap.empty[String, String]

  def add(key: String, value: String): IO[Unit] = IO.delay:
    appendLog.add(key, value)
    memTable += (key -> value)
    if memTable.size >= maxSize then flush()

  def find(key: String): IO[Option[String]] = IO.delay:
    memTable.get(key).orElse(fileStorage.find(key))

  def flush(): IO[Unit] = IO.delay:
    fileStorage.add(memTable)
    memTable.clear()
    appendLog.clear()

  def mergeAndCompact(): IO[Unit] = IO.delay:
    fileStorage.mergeAndCompact()

  def restore(): IO[Unit] =
    IO.raiseUnless(memTable.isEmpty)(
      new RuntimeException("Cannot restore state when it's already initialized")
    ).flatTap: _ =>
      appendLog.read().traverse(add)

class MultithreadedStorageEngine(engine: SimpleStorageEngine, mutex: Mutex[IO])
    extends StorageEngine:

  override def add(key: String, value: String): IO[Unit] =
    mutex.lock.surround:
      engine.add(key, value)

  override def find(key: String): IO[Option[String]] =
    engine.find(key)

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
