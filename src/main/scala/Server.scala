import TapirEndpoints.Margin.Margin
import cats.effect.{ContextShift, ExitCode, IO, IOApp, Sync, Timer}
import fs2.Stream
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import sttp.tapir.json.circe.TapirJsonCirce
import sttp.tapir.{Schema, SchemaType, Validator}

object Server extends IOApp {

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

object TapirEndpoints extends TapirJsonCirce{
  import io.circe._
  import io.circe.generic.auto._
  import sttp.tapir._

  implicit val enumDecoder: Decoder[Margin.Value] = Decoder.decodeEnumeration(Margin)
  implicit val enumEncoder: Encoder[Margin.Value] = Encoder.encodeEnumeration(Margin)
  implicit val schemaForEnum: Schema[Margin.Value] = Schema(SchemaType.SString)
  implicit def validatorForEnum: Validator[Margin.Value] = Validator.`enum`(Margin.values.toList, v=> Option(v))


  type Limit = Int
  type AuthToken = String

  object Margin extends Enumeration {
    type Margin = Value
    val TOP, BOTTOM, LEFT = Value
  }

  final case class BooksFromYear(genre: String, year: Int)

  case class MoreNestedClass(y: Set[Margin])
  case class NestedClass(x: Set[Margin], y: MoreNestedClass)
  case class Working(title: String, margin: Margin, x: NestedClass)
  case class NotWorking(title: String, margin: Margin, x: Set[NestedClass])

  def compilesAlways: Endpoint[(BooksFromYear, Limit, AuthToken), String, Working, Nothing] = {
    endpoint
      .get
      .in(("books" / path[String]("genre") / path[Int]("year")).mapTo(BooksFromYear))
      .in(query[Limit]("limit").description("Maximum number of books to retrieve"))
      .in(header[AuthToken]("X-Auth-Token"))
      .errorOut(stringBody)
      .out(jsonBody[Working])
  }

  def compilesOnlyInSomeConfigurationsDescribedInBuildsbt: Endpoint[(BooksFromYear, Limit, AuthToken), String, NotWorking, Nothing] = {
    endpoint
      .get
      .in(("books" / path[String]("genre") / path[Int]("year")).mapTo(BooksFromYear))
      .in(query[Limit]("limit").description("Maximum number of books to retrieve"))
      .in(header[AuthToken]("X-Auth-Token"))
      .errorOut(stringBody)
      .out(jsonBody[NotWorking])
  }
}

object models {
  trait EnumHelper { e: Enumeration =>
    import io.circe._

    implicit val enumDecoder: Decoder[e.Value] = Decoder.decodeEnumeration(e)
    implicit val enumEncoder: Encoder[e.Value] = Encoder.encodeEnumeration(e)

    implicit val schemaForEnum: Schema[e.Value] = Schema(SchemaType.SString)
    implicit def validatorForEnum: Validator[e.Value] = Validator.`enum`(e.values.toList, v => Option(v))
  }
}