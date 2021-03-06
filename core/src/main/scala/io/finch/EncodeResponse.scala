package io.finch

import com.twitter.io.Buf

/**
 * An abstraction that is responsible for encoding the response of type `A`.
 */
trait EncodeResponse[-A] {
  def apply(rep: A): Buf
  def contentType: String
  def charset: Option[String] = Some("utf-8")
}

trait LowPriorityEncodeResponseInstances {

  /**
   * Convenience method for creating new [[EncodeResponse]] instances.
   */
  def apply[A](ct: String, cs: Option[String] = Some("utf-8"))(fn: A => Buf): EncodeResponse[A] =
    new EncodeResponse[A] {
      override def apply(rep: A): Buf = fn(rep)
      override def contentType: String = ct
      override def charset: Option[String] = cs
    }

  /**
   * Convenience method for creating new [[EncodeResponse]] instances that treat String contents.
   */
  def fromString[A](ct: String, cs: Option[String] = Some("utf-8"))(fn: A => String): EncodeResponse[A] =
    apply(ct, cs)(fn.andThen(Buf.Utf8.apply))

  implicit val encodeException: EncodeResponse[Exception] =
    fromString("text/plain")(e => Option(e.getMessage).getOrElse(""))
}

object EncodeResponse extends LowPriorityEncodeResponseInstances {

  /**
   * Converts [[EncodeAnyResponse]] into [[EncodeResponse]].
   */
  implicit def anyToConcreteEncode[A](implicit e: EncodeAnyResponse): EncodeResponse[A] =
    new EncodeResponse[A] {
      def apply(rep: A): Buf = e(rep)
      def contentType: String = e.contentType
    }

  implicit val encodeString: EncodeResponse[String] =
    fromString[String]("text/plain")(identity)

  implicit val encodeUnit: EncodeResponse[Unit] =
    apply("text/plain")(_ => Buf.Empty)

  implicit val encodeBuf: EncodeResponse[Buf] =
    apply("application/octet-stream", None)(identity)
}

/**
 * An abstraction that is responsible for encoding the response of a generic type.
 */
trait EncodeAnyResponse {
  def apply[A](rep: A): Buf
  def contentType: String
}
