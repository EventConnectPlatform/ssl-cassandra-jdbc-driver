/*
 Derived from EventConnect internal codebase and stripped down by Eray Ocak
 */

package com.eventconnect.services.configuration.security

import org.apache.http.ssl.PrivateKeyStrategy
import org.apache.http.ssl.SSLContextBuilder
import org.apache.http.ssl.TrustStrategy
import java.io.File
import javax.net.ssl.SSLContext

object ECSSLContextUtil {
    fun sslContext(block: SSLContextBuilder.() -> Unit): SSLContext {
        return SSLContextBuilder.create().apply(block).build()
    }

    fun SSLContextBuilder.applyKeystorePair(
        pair: Pair<File, String>,
        keyStrategy: PrivateKeyStrategy?
    ): SSLContextBuilder {
        return this.loadKeyMaterial(pair.first, pair.second.toCharArray(), pair.second.toCharArray(), keyStrategy)
    }

    fun SSLContextBuilder.applyTruststorePair(
        pair: Pair<File, String>,
        trustStrategy: TrustStrategy?
    ): SSLContextBuilder {
        return this.loadTrustMaterial(pair.first, pair.second.toCharArray(), trustStrategy)
    }
}