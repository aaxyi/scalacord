package dev.axyria.scalacord.common.datatype

import cats.instances.finiteDuration
import dev.axyria.scalacord.common.util.ZeroType
import io.circe.Decoder
import io.circe.Decoder.Result
import io.circe.Encoder
import io.circe.HCursor
import io.circe.Json
import scala.concurrent.duration.DurationInt
import scala.concurrent.duration.DurationLong
import scala.concurrent.duration.FiniteDuration
import spire.math.UByte
import spire.math.ULong

type Snowflake = Snowflake.Type

/** An unique identifier for Discord entities.
  *
  * Snowflakes are a kind of entity identification format released originally by
  * [[https://twitter.com Twitter]] in late 2010, then being utilized in companies
  * [https://discord.com/developers/docs/reference#snowflakes like Discord] in their internal
  * structuring. A snowflake is nothing more than a 64-bit unsigned integer (Long) ID with a
  * [[timestamp]], which can make them comparable to others. This case class implements [[ZeroType]]
  * in order to be considered a value class, which reduces the performance overhead over
  * instantiating new objects unnecessarily.
  *
  * Many of the functions implemented here are taken from
  * [[https://discord.com/developers/docs/reference#snowflakes-snowflake-id-format-structure-left-to-right Discord's documentation reference]].
  */
object Snowflake extends ZeroType[ULong] {

    /** The milliseconds representation of Discord Epoch (2015). */
    val Epoch: ULong = ULong(1420070400000L)

    /** The minimum value for a snowflake. */
    val MinValue = apply(Epoch.toLong.milliseconds).toOption.get

    /** The maximum value for a snowflake. */
    val MaxValue = Snowflake(ULong(Long.MaxValue))

    /** Builds a snowflake identifier from a timestamp. */
    def apply(timestamp: FiniteDuration): Either[IllegalArgumentException, Snowflake] =
        val long = timestamp.toMillis
        if long < ULong.MinValue.toLong then
            return Left(new IllegalArgumentException("Timestamp is too low!"))
        if long > Long.MaxValue.toLong then
            return Left(new IllegalArgumentException("Timestamp is too high!"))
        Right(Snowflake(ULong((long - Epoch.toLong) << 22)))

    extension (self: Type)
        /** Returns how many milliseconds has passed since 2015 until this snowflake was generated.
          */
        def millisecondsSinceDiscordEpoch: Long = self.value.toLong >> 22L

        /** Returns the instant over the milliseconds since Discord Epoch, the first second of 2015
          * or 1420070400000.
          */
        def timestamp: FiniteDuration =
            (self.millisecondsSinceDiscordEpoch + Epoch.toLong).milliseconds

        /** Returns the internal worker ID of this snowflake. */
        def internalWorkerId: UByte = UByte(((self.value.toLong & 0x3e0000) >> 17).toByte)

        /** Returns the internal process ID of this snowflake. */
        def internalProcessId: UByte = UByte(((self.value.toLong & 0x1f000) >> 12).toByte)

        /** Returns the increment identification of this snowflake. */
        def incrementSize: Int = (self.value.toLong & 0xfff).toInt

        // credits for this workaround: kordlib/kord
        // snowflake timestamps aren't 100% accurate, it can be off by 1ms
        def matches(duration: FiniteDuration): Boolean =
            val delta = 1.millisecond - 1.nanosecond
            val range = (duration.toNanos - delta.toNanos) to (duration.toNanos + delta.toNanos)
            range.contains(self.timestamp.toNanos)

    export dev.axyria.scalacord.common.datatype.snowflakeDecoder
    export dev.axyria.scalacord.common.datatype.snowflakeEncoder
}

given snowflakeOrdered: Ordering[Snowflake] with
    final def compare(x: Snowflake, y: Snowflake): Int =
        x.millisecondsSinceDiscordEpoch.compare(y.millisecondsSinceDiscordEpoch)

given snowflakeEncoder: Encoder[Snowflake] with
    final def apply(snowflake: Snowflake): Json = Json.fromLong(snowflake.value.toLong)

given snowflakeDecoder: Decoder[Snowflake] with
    final def apply(cursor: HCursor): Result[Snowflake] =
        cursor.as[Long].map(long => Snowflake(ULong(long)))
