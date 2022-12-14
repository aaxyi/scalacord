package dev.axyria.scalacord.gateway.job

import cats.effect.kernel.Sync
import cats.syntax.all.*
import dev.axyria.scalacord.common.datatype.Optional
import dev.axyria.scalacord.gateway.DiscordGateway
import dev.axyria.scalacord.gateway.decoder.CompressionKind
import dev.axyria.scalacord.gateway.payload.GatewayPayload
import dev.axyria.scalacord.gateway.payload.command.Identify
import dev.axyria.scalacord.gateway.payload.command.IdentifyCodec
import dev.axyria.scalacord.gateway.payload.event.Event
import dev.axyria.scalacord.gateway.payload.event.Hello
import dev.axyria.scalacord.gateway.payload.event.Ready
import fs2.Stream

/** A job for providing our identity to Gateway as a step of authentication.
  *
  * @tparam F
  *   A type-class able to suspend side effects into the [[F]] context.
  */
case class LoginJob[F[_]: Sync](client: DiscordGateway[F]) extends GatewayJob[F] {
    override def entrypoint(payload: GatewayPayload, event: Stream[F, Event]): Stream[F, Unit] =
        event
            .map(event => event.data)
            .flatMap {
                case _: Hello =>
                    Stream
                        .emit(
                            Identify(
                                token = client.settings.token,
                                shard = Optional.keepOrMiss(client.settings.shard),
                                compress = client.settings.io.kind == CompressionKind.Payload,
                                largeThreshold = client.settings.largeThreshold,
                                intents = client.settings.intents,
                                presence = Optional.keepOrMiss(client.settings.presence),
                            )
                        )
                        .evalMap(identify => client.send(IdentifyCodec.toPayload(identify)))
                case _ => Stream.empty
            }
}
