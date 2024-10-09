package scuffedb

import java.nio.file.Paths

import scala.concurrent.duration.*

import cats.effect.*
import com.comcast.ip4s.*
import fs2.Stream

import codecs.given

object Main extends IOApp.Simple:
  def run: IO[Unit] =
    IO.fromEither(FileStorage(Paths.get("./data")))
      .flatMap: fileStorage =>
        MultithreadedStorageEngine(
          SimpleStorageEngine(
            maxSize = 6,
            fileStorage = fileStorage,
            appendLog = AppendLog(Paths.get("./appendlog.log"))
          )
        ).flatMap: engine =>
          val server = http.makeServer(http.makeRoutes(engine))
          val periodicCleanup =
            Stream.awakeEvery[IO](5.minutes)
              >> Stream.eval(engine.mergeAndCompact())

          engine.restore() *> Stream(
            periodicCleanup,
            Stream.eval(server.useForever)
          ).parJoinUnbounded.compile.drain.as(ExitCode.Success)
