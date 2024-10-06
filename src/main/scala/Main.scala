import java.nio.file.Paths

import scala.concurrent.duration.*

import cats.effect.*
import com.comcast.ip4s.*
import org.http4s.HttpRoutes
import org.http4s.dsl.io.*
import org.http4s.ember.server.*
import org.http4s.implicits.*
import fs2.Stream

object Main extends IOApp.Simple:
  def run: IO[Unit] =
    FileStorage(Paths.get("./data")) match
      case Left(error) => IO.println(error)
      case Right(fileStorage) =>
        MultithreadedStorageEngine(
          SimpleStorageEngine(
            maxSize = 6,
            fileStorage = fileStorage,
            appendLog = AppendLog(Paths.get("./appendlog.log"))
          )
        ).flatMap: engine =>
          val server = EmberServerBuilder
            .default[IO]
            .withHost(ipv4"0.0.0.0")
            .withPort(port"8080")
            .withHttpApp(routes(engine).orNotFound)
            .build
          val periodicCleanup =
            Stream.awakeEvery[IO](5.minutes)
              >> Stream.eval(engine.mergeAndCompact())

          engine.restore() *> Stream(
            periodicCleanup,
            Stream.eval(server.useForever)
          ).parJoinUnbounded.compile.drain.as(ExitCode.Success)
