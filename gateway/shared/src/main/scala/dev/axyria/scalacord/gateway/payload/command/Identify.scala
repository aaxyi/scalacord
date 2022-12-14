package dev.axyria.scalacord.gateway.payload.command

import dev.axyria.scalacord.common.datatype.*
import dev.axyria.scalacord.common.entity.PresenceUpdate
import dev.axyria.scalacord.common.entity.Shard
import dev.axyria.scalacord.common.util.*
import dev.axyria.scalacord.gateway.datatype.Intent
import dev.axyria.scalacord.gateway.payload.PayloadData
import io.circe.Decoder
import io.circe.Encoder
import io.circe.HCursor
import io.circe.Json
import io.circe.generic.semiauto.*

/** An initial handshake with the Gateway that's required before the app can begin sending or
  * receiving most Gateway events.
  */
object IdentifyCodec extends CommandPayloadCodec[Identify] {
    override def opCode: Int = 2

    override def encoder: Encoder[Identify] = identifyEncoder

    override def decoder: Decoder[Identify] = identifyDecoder
}

/** An initial handshake with the Gateway that's required before the app can begin sending or
  * receiving most Gateway events.
  * @param token
  *   The authentication token.
  * @param shard
  *   Used for Guild Sharding.
  * @param compress
  *   Whether this connection supports compression of packets.
  * @param intents
  *   Gateway Intents you wish to receive.
  * @param largeThreshold
  *   Value between 50 and 250, total number of members where the gateway will stop sending offline
  *   members in the guild member list.
  * @param properties
  *   The connection properties.
  * @param presence
  *   Presence structure for initial presence information.
  */
case class Identify(
    token: String,
    shard: Optional[Shard] = Optional.missing,
    compress: Boolean = false,
    intents: List[Intent] = List(Intent.Guilds),
    largeThreshold: Int = 50,
    properties: IdentifyProperties = IdentifyProperties(),
    presence: Optional[PresenceUpdate] = Optional.missing,
) extends PayloadData

case class IdentifyProperties(
    os: String = System.getProperty("os.name"),
    browser: String = "Scalacord",
    device: String = "Scalacord"
)

object IdentifyProperties {
    given encoder: Encoder[IdentifyProperties] = deriveEncoder
    given decoder: Decoder[IdentifyProperties] = deriveDecoder
}

object Identify {
    export dev.axyria.scalacord.gateway.payload.command.identifyDecoder
    export dev.axyria.scalacord.gateway.payload.command.identifyEncoder
}

given identifyEncoder: Encoder[Identify] with
    final def apply(identify: Identify): Json =
        val elems: List[Option[EncodingContext[?]]] =
            ("token", identify.token).context
                :: ("properties", identify.properties).context
                :: ("compress", identify.compress).context
                :: ("large_threshold", identify.largeThreshold).context
                :: ("shard", identify.shard).optionContext
                :: ("presence", identify.presence).optionContext
                :: ("intents", identify.intents).context
                :: Nil
        elems.withOptional

given identifyDecoder: Decoder[Identify] with
    final def apply(cursor: HCursor): Decoder.Result[Identify] =
        for {
            token          <- cursor.get[String]("token")
            properties     <- cursor.get[IdentifyProperties]("properties")
            compress       <- cursor.get[Boolean]("compress")
            largeThreshold <- cursor.get[Int]("large_threshold")
            shard          <- cursor.get[Optional[Shard]]("shard")
            presence       <- cursor.get[Optional[PresenceUpdate]]("presence")
            intents        <- cursor.get[List[Intent]]("intents")
        } yield Identify(
            token,
            shard,
            compress,
            intents,
            largeThreshold,
            properties,
            presence
        )
