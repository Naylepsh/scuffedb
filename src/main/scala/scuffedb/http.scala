package scuffedb
package http

import cats.effect.IO
import com.comcast.ip4s.*
import org.http4s.dsl.io.*
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.{HttpApp, HttpRoutes}

def makeRoutes(engine: StorageEngine): HttpRoutes[IO] = HttpRoutes.of[IO]:
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

  case DELETE -> Root / key =>
    engine.delete(key) *> Ok("")

def makeServer(routes: HttpRoutes[IO]) = EmberServerBuilder
  .default[IO]
  .withHost(ipv4"0.0.0.0")
  .withPort(port"8080")
  .withHttpApp(routes.orNotFound)
  .build
