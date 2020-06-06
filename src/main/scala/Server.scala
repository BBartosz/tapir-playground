import java.util.concurrent.{Executors, TimeUnit}

import TapirEndpoints.Margin.Margin
import cats.data.NonEmptyList
import cats.effect.{ContextShift, ExitCode, IO, IOApp, Resource, Sync, SyncIO, Timer}
import fs2.Stream
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder

import scala.concurrent.ExecutionContext

object Server extends IOApp.WithContext {
  private val threadsMultiplier = 2
  override protected def executionContextResource: Resource[SyncIO, ExecutionContext] = {
    Resource
      .make(SyncIO(Executors.newFixedThreadPool(Runtime.getRuntime.availableProcessors() * threadsMultiplier)))(pool =>
        SyncIO {
          pool.shutdown()
          pool.awaitTermination(10, TimeUnit.SECONDS)
        }
      )
      .map(ExecutionContext.fromExecutorService)
  }

  override def run(args: List[String]): IO[ExitCode] = {
    implicitly[Timer[IO]]
    implicitly[ContextShift[IO]]

    val helloEndpoints = new HelloEndpoints[IO]
    for{
      exitCode <- Http.stream[IO](helloEndpoints).compile.drain.as(ExitCode.Success)
    } yield exitCode
  }
}

object Http {
  import cats.effect._
  import org.http4s._
  import org.http4s.implicits._
  def app[F[_]: Effect: ContextShift: Timer](helloEndpoints: HelloEndpoints[F]): HttpApp[F] =
    Router(
      "/" -> helloEndpoints.endpoints
    ).orNotFound

//  def tapirApp[F[_]: Effect: ContextShift: Timer](helloEndpoints: HelloEndpoints[F]): HttpApp[F] ={
//    val routes = NonEmptyList.of(TapirEndpoints.booksListing, ???)
//    routes.reduceK
//  }

  def stream[F[_]: ConcurrentEffect: Timer: ContextShift](helloEndpoints: HelloEndpoints[F]): Stream[F, ExitCode] =
    BlazeServerBuilder[F]
      .bindHttp(port = 8080, host = "0.0.0.0")
      .withHttpApp(app[F](helloEndpoints))
      .serve

}


class HelloEndpoints[F[_]: Sync]() extends Http4sDsl[F] {
  import cats.effect._
  import cats.implicits._
  import org.http4s._

  def endpoints = HttpRoutes.of[F] {
    case GET -> Root / "hello" / name => Sync[F].delay(s"hello $name").flatMap(Ok(_))
    case GET -> Root / "hello2" / name => Sync[F].delay(s"hello2 $name").flatMap(Ok(_))
  }
}

object TapirEndpoints {
  import sttp.tapir._
  import sttp.tapir.json.circe._
  import io.circe.generic.auto._
  import io.circe._
  implicit val enumDecoder: Decoder[Margin.Value] = Decoder.decodeEnumeration(Margin)
  implicit val enumEncoder: Encoder[Margin.Value] = Encoder.encodeEnumeration(Margin)

  implicit val schemaForEnum: Schema[Margin.Value] = Schema(SchemaType.SString)
  implicit def validatorForEnum: Validator[Margin.Value] = Validator.`enum`(Margin.values.toList, v => Option(v))

  type Limit = Int
  type AuthToken = String

  object Margin extends Enumeration {
    type Margin = Value
    val TOP, BOTTOM, LEFT, RIGHT = Value
  }
  case class MoreNestedClass(y: Set[Margin])
  case class NestedClass(x: Set[Margin.Value], y: MoreNestedClass)
  case class BooksFromYear(genre: String, year: Int)
  case class Book(title: String, margin: Margin, x: NestedClass)

  def booksListing: Endpoint[(BooksFromYear, Limit, AuthToken), String, List[Book], Nothing] =
    endpoint
      .get
      .in(("books" / path[String]("genre") / path[Int]("year")).mapTo(BooksFromYear))
      .in(query[Limit]("limit").description("Maximum number of books to retrieve"))
      .in(header[AuthToken]("X-Auth-Token"))
      .errorOut(stringBody)
      .out(jsonBody[List[Book]])
}