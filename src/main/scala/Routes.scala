import cats.effect.*
import org.http4s.*
import org.http4s.dsl.io.*

def routes(engine: StorageEngine): HttpRoutes[IO] = HttpRoutes.of[IO]:
  case GET -> Root / key =>
    engine
      .find(key)
      .flatMap:
        case None        => NotFound()
        case Some(value) => Ok(value)

  case req @ POST -> Root / key =>
    req
      .as[String]
      .flatMap: value =>
        engine.add(key, value) *> Ok("")
