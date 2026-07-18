package org.etrange.towards.plugins

import io.ktor.server.auth.AuthenticationConfig
import io.ktor.server.auth.AuthenticationContext
import io.ktor.server.auth.AuthenticationProvider
import org.etrange.towards.domain.model.ActorContext
import org.etrange.towards.domain.model.UserId

data class DummyPrincipal(
    val userId: UserId,
) {
    fun toActorContext() = ActorContext(userId)
}

class DummyAuthenticationProvider internal constructor(
    config: Config,
) : AuthenticationProvider(config) {
    private val userId = requireNotNull(config.userId) { "A dummy user id must be configured" }

    class Config internal constructor(name: String?) : AuthenticationProvider.Config(name) {
        var userId: UserId? = null
    }

    override suspend fun onAuthenticate(context: AuthenticationContext) {
        context.principal(DummyPrincipal(userId))
    }
}

fun AuthenticationConfig.dummy(
    name: String? = "dummy",
    configure: DummyAuthenticationProvider.Config.() -> Unit,
) {
    val config = DummyAuthenticationProvider.Config(name).apply(configure)
    register(DummyAuthenticationProvider(config))
}
