/*
 Derived from EventConnect internal codebase and stripped down by Eray Ocak
 */

package com.eventconnect.services.constant.internal

import com.eventconnect.services.env
import java.io.File

object SecretMapping {
    object Key {
        const val cassClientKeystore = "CASS_CLIENT_KEYSTORE"
        const val cassClientKeystorePassword = "CASS_CLIENT_KEYSTORE_PASSWORD"

        const val cassClientTruststore = "CASS_CLIENT_TRUSTSTORE"
        const val cassClientTruststorePassword = "CASS_CLIENT_TRUSTSTORE_PASSWORD"

        const val cassClientCipherSuite = "CASS_CLIENT_CIPHER_SUITE"
    }

    object Volume {}

    object File {
        val cassClientKeystore = Env.CassClientSsl.cassClientKeystore?.let {
            File(it)
        }

        val cassClientTruststore = Env.CassClientSsl.cassClientTruststore?.let {
            File(it)
        }
    }

    @Suppress("SimpleRedundantLet")
    object Env {
        object CassClientSsl {
            val cassClientKeystore = env[Key.cassClientKeystore]
            val cassClientKeystorePassword = env[Key.cassClientKeystorePassword]

            val cassClientTruststore = env[Key.cassClientTruststore]
            val cassClientTruststorePassword = env[Key.cassClientTruststorePassword]

            val cassClientCipherSuite = env[Key.cassClientCipherSuite]?.let {
                it.split(",").map { cipher -> cipher.trim() }.toTypedArray<String>()
            } ?: arrayOf("TLS_RSA_WITH_AES_128_CBC_SHA", "TLS_RSA_WITH_AES_256_CBC_SHA")
        }

        object Cassandra {}
    }
}