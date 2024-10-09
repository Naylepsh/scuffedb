package scuffedb
package codecs

import scala.util.control.NoStackTrace

import domain.*

case class DecodingFailure(reason: String) extends NoStackTrace

trait Codec[A]:
  def encode(value: A): String
  def decode(encoded: String): Either[DecodingFailure, A]

given Codec[Mark] with
  override def encode(value: Mark): String =
    value match
      case Mark.Active    => "a"
      case Mark.Tombstone => "t"

  override def decode(encoded: String): Either[DecodingFailure, Mark] =
    encoded match
      case "a" => Right(Mark.Active)
      case "t" => Right(Mark.Tombstone)
      case other =>
        Left(DecodingFailure(s"Invalid value '${other}' for Mark"))

given entryCodec(using statusCodec: Codec[Mark]): Codec[Entry] with
  override def encode(entry: Entry): String =
    s"${entry.key}:${statusCodec.encode(entry.mark)}:${entry.value}"

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
