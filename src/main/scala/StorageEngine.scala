import scala.collection.mutable.TreeMap

class StorageEngine(maxSize: Long, fileStorage: FileStorage):
  val memTable = TreeMap.empty[String, String]

  def add(key: String, value: String): Unit =
    memTable += (key -> value)
    if memTable.size >= maxSize then flush()

  def find(key: String): Option[String] =
    memTable.get(key).orElse(fileStorage.find(key))

  def flush(): Unit =
    fileStorage.add(memTable)
    memTable.clear()

  def mergeAndCompact(): Unit =
    fileStorage.mergeAndCompact()
