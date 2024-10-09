import scala.util.control.NoStackTrace

case class DecodingFailure(reason: String) extends NoStackTrace

trait Codec[A]:
  def encode(value: A): String
  def decode(encoded: String): Either[DecodingFailure, A]

object Codec:
  given Codec[Status] with
    override def encode(value: Status): String =
      value match
        case Status.Active   => "a"
        case Status.Inactive => "i"

    override def decode(encoded: String): Either[DecodingFailure, Status] =
      encoded match
        case "a" => Right(Status.Active)
        case "i" => Right(Status.Inactive)
        case other =>
          Left(DecodingFailure(s"Invalid value '${other}' for Status"))

  given entryCodec(using statusCodec: Codec[Status]): Codec[Entry] with
    override def encode(entry: Entry): String =
      s"${entry.key}:${statusCodec.encode(entry.status)}:${entry.value}"

    override def decode(encoded: String): Either[DecodingFailure, Entry] =
      encoded.split(":") match
        case Array(key, status, value) =>
          statusCodec.decode(status).map(Entry(key, value, _))
        case other =>
          Left(
            DecodingFailure(
              s"Invalid value '${other.toList.take(24)}...' for Entry"
            )
          )

