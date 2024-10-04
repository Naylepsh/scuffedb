import java.nio.file.Paths

import cats.effect.*
import com.comcast.ip4s.*
import org.http4s.HttpRoutes
import org.http4s.dsl.io.*
import org.http4s.ember.server.*
import org.http4s.implicits.*

object Main extends IOApp.Simple:
  def run: IO[Unit] =
    FileStorage(Paths.get("./data")) match
      case Left(error) => IO.println(error)
      case Right(fileStorage) =>
        val engine  = StorageEngine(maxSize = 6, fileStorage = fileStorage)
        val httpApp = routes(engine).orNotFound

        EmberServerBuilder
          .default[IO]
          .withHost(ipv4"0.0.0.0")
          .withPort(port"8080")
          .withHttpApp(httpApp)
          .build
          .use(_ => IO.never)
          .as(ExitCode.Success)
