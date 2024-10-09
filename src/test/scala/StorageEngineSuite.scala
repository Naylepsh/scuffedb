import java.nio.file.Paths
import cats.effect.IO

class StorageEngineSuite extends munit.CatsEffectSuite:
  test("integration"):
    StorageEngineSuite.resources.flatMap: (_, log, engine) =>
      for
        x1 <- engine.find("x")
        _ = log.add(Entry.makeActive("x", "hello"))
        _ = log.add(Entry.makeActive("y", "world"))
        _  <- engine.restore()
        x2 <- engine.find("x")
        y1 <- engine.find("y")
        _  <- engine.add("z", "goodbye")
        z1 <- engine.find("z")
        _  <- engine.flush()
        x3 <- engine.find("x")
        y2 <- engine.find("y")
        z2 <- engine.find("z")
      yield
        assertEquals(x1, None)
        assertEquals(x2, Some("hello"))
        assertEquals(y1, Some("world"))
        assertEquals(z1, Some("goodbye"))
        assertEquals(x3, Some("hello"))
        assertEquals(y2, Some("world"))
        assertEquals(z2, Some("goodbye"))

object StorageEngineSuite:
  val resources =
    FileStorage(Paths.get("./src/test/resources/data")) match
      case Left(error) => IO.raiseError(new RuntimeException(error.toString))
      case Right(fileStorage) =>
        IO.delay:
          val log = AppendLog(Paths.get("./src/test/resources/appendlog.log"))
          val engine = SimpleStorageEngine(
            maxSize = 6,
            fileStorage = fileStorage,
            appendLog = log
          )

          log.clear()
          fileStorage.clear()

          (fileStorage, log, engine)
