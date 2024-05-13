package com.dbschema

import com.datastax.driver.core.*
import com.eventconnect.services.configuration.security.ECSSLContextUtil
import com.eventconnect.services.configuration.security.ECSSLContextUtil.applyTruststorePair
import com.eventconnect.services.configuration.security.ECSSLContextUtil.applyKeystorePair
import com.eventconnect.services.constant.internal.SecretMapping
import com.google.common.base.Strings
import org.apache.http.conn.ssl.TrustAllStrategy
import java.io.File
import java.net.InetAddress
import java.net.MalformedURLException
import java.net.URL
import java.net.UnknownHostException
import java.util.*
import java.util.logging.Logger

class CassandraClientURI(uri: String, info: Properties?) {
    /**
     * Gets the list of hosts
     *
     * @return the host list
     */
    @JvmField
    var hosts: List<String>? = null

    /**
     * Gets the keyspace name
     *
     * @return the keyspace name
     */
    @JvmField
    var keyspace: String? = null

    /**
     * Gets the collection name
     *
     * @return the collection name
     */
    var collection: String? = null

    /**
     * Get the unparsed URI.
     *
     * @return the URI
     */
    val uRI: String

    /**
     * Gets the username
     *
     * @return the username
     */
    val username: String?

    /**
     * Gets the password
     *
     * @return the password
     */
    @JvmField
    val password: String?

    /**
     * Gets the ssl enabled property
     *
     * @return the ssl enabled property
     */
    @JvmField
    val sslEnabled: Boolean
    private val verifyServerCert: Boolean

    @JvmField
    val consistencyLevel: ConsistencyLevel

    init {
        var uri = uri
        this.uRI = uri
        require(uri.startsWith(PREFIX)) { "URI needs to start with $PREFIX" }

        uri = uri.substring(PREFIX.length)


        var serverPart: String
        var nsPart: String?
        var options: Map<String?, MutableList<String?>?>? = null

        run {
            val lastSlashIndex = uri.lastIndexOf("/")
            if (lastSlashIndex < 0) {
                require(!uri.contains("?")) { "URI contains options without trailing slash" }
                serverPart = uri
                nsPart = null
            } else {
                serverPart = uri.substring(0, lastSlashIndex)
                nsPart = uri.substring(lastSlashIndex + 1)

                val questionMarkIndex = nsPart!!.indexOf("?")
                if (questionMarkIndex >= 0) {
                    options = parseOptions(nsPart!!.substring(questionMarkIndex + 1))
                    nsPart = nsPart!!.substring(0, questionMarkIndex)
                }
            }
        }

        this.username = getOption(info, options, "user", null)
        this.password = getOption(info, options, "password", null)
        val sslEnabledOption =
            getOption(info, options, DriverPropertyInfoHelper.ENABLE_SSL, DriverPropertyInfoHelper.ENABLE_SSL_DEFAULT)
        this.sslEnabled = DriverPropertyInfoHelper.isTrue(sslEnabledOption)
        val verifyServerCertOption = getOption(
            info,
            options,
            DriverPropertyInfoHelper.VERIFY_SERVER_CERTIFICATE,
            DriverPropertyInfoHelper.VERIFY_SERVER_CERTIFICATE_DEFAULT
        )
        this.verifyServerCert = DriverPropertyInfoHelper.isTrue(verifyServerCertOption)
        val consistencyLevelOption = getOption(
            info,
            options,
            DriverPropertyInfoHelper.CONSISTENCY_LEVEL,
            DriverPropertyInfoHelper.CONSISTENCY_LEVEL_DEFAULT
        )
        var consistencyLevel = try {
            ConsistencyLevel.valueOf(consistencyLevelOption.uppercase())
        } catch (ignored: IllegalArgumentException) {
            QueryOptions.DEFAULT_CONSISTENCY_LEVEL
        }
        this.consistencyLevel = consistencyLevel


        run {
            // userName,password,hosts
            val all: MutableList<String> = LinkedList()

            Collections.addAll(all, *serverPart.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())
            hosts = Collections.unmodifiableList(all)
        }

        if (nsPart != null && nsPart!!.length != 0) { // keyspace._collection
            val dotIndex = nsPart!!.indexOf(".")
            if (dotIndex < 0) {
                keyspace = nsPart
                collection = null
            } else {
                keyspace = nsPart!!.substring(0, dotIndex)
                collection = nsPart!!.substring(dotIndex + 1)
            }
        } else {
            keyspace = null
            collection = null
        }
    }

    /**
     * @return option from properties or from uri if it is not found in properties.
     * null if options was not found.
     */
    private fun getOption(
        properties: Properties?,
        options: Map<String?, MutableList<String?>?>?,
        optionName: String,
        defaultValue: String?
    ): String {
        if (properties != null) {
            val option = properties[optionName] as String?
            if (option != null) {
                return option
            }
        }
        val value = getLastValue(options, optionName)
        return value ?: defaultValue!!
    }

    @Suppress("UNCHECKED_CAST")
    @Throws(UnknownHostException::class, SSLParamsException::class)
    fun createCluster(): Cluster {
        logger.info("Creating cluster")
        val builder = Cluster.builder()
        var port = -1
        for (hostItem in hosts!!) {
            var host = hostItem

            val idx = host.indexOf(":")
            if (idx > 0) {
                port = host.substring(idx + 1).trim { it <= ' ' }.toInt()
                host = host.substring(0, idx).trim { it <= ' ' }
            }
            logger.info("Adding contact point: $host with port $port")
            builder.addContactPoints(InetAddress.getByName(host))

            val enableSsl = SecretMapping.Env.CassClientSsl.cassClientEnableSsl

            logger.info("Enable SSL: $enableSsl")
            if (enableSsl) {
                logger.info("Configuring SSL")
                val truststore = Pair(
                    SecretMapping.File.cassClientTruststore,
                    SecretMapping.Env.CassClientSsl.cassClientTruststorePassword
                ).takeIf { it.first != null && it.second != null }

                val keystore = Pair(
                    SecretMapping.File.cassClientKeystore,
                    SecretMapping.Env.CassClientSsl.cassClientKeystorePassword
                ).takeIf { it.first != null && it.second != null }


                if (truststore == null) logger.warning("Truststore or it's password is not defined. Truststore won't be applied to SSLContext.")
                if (keystore == null) logger.warning("Keystore or it's password is not defined. Keystore won't be applied to SSLContext.")

                val options: SSLOptions = RemoteEndpointAwareJdkSSLOptions.builder().withSSLContext(
                    ECSSLContextUtil.sslContext {
                        if (truststore != null) applyTruststorePair(
                            truststore as Pair<File, String>,
                            TrustAllStrategy.INSTANCE
                        )
                        if (keystore != null) applyKeystorePair(keystore as Pair<File, String>) { aliases, _ ->
                            SecretMapping.Env.CassClientSsl.cassClientKeyAlias?.also {alias ->
                                if (aliases.containsKey(alias)) return@applyKeystorePair alias else throw IllegalArgumentException("Keystore alias was explicitly defined but not found in keystore: $alias")
                            }
                            if (aliases.containsKey("client")) "client" else {
                                aliases.entries.firstOrNull()?.key
                                    ?: throw IllegalArgumentException("No alias found in keystore")
                            }
                        }
                    }
                ).withCipherSuites(SecretMapping.Env.CassClientSsl.cassClientCipherSuite)
                    .build()

                builder.withSSL(options)
            }
        }
        if (port > -1) {
            builder.withPort(port)
        }
        if (username != null && !username.isEmpty() && password != null) {
            builder.withCredentials(username, password)
            println("Using authentication as user '$username'")
        }
        return builder.build()
    }


    private fun getLastValue(optionsMap: Map<String?, MutableList<String?>?>?, key: String): String? {
        if (optionsMap == null) return null
        val valueList: List<String?>? = optionsMap[key.lowercase()]
        if (valueList == null || valueList.size == 0) return null
        return valueList[valueList.size - 1]
    }

    private fun parseOptions(optionsPart: String): Map<String?, MutableList<String?>?> {
        val optionsMap: MutableMap<String?, MutableList<String?>?> = HashMap()

        for (_part in optionsPart.split("[&;]".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
            val idx = _part.indexOf("=")
            if (idx >= 0) {
                val key = _part.substring(0, idx).lowercase()
                val value = _part.substring(idx + 1)
                var valueList = optionsMap[key]
                if (valueList == null) {
                    valueList = ArrayList(1)
                }
                valueList.add(value)
                optionsMap[key] = valueList
            }
        }

        return optionsMap
    }


    // ---------------------------------


    override fun toString(): String {
        return uRI
    }

    companion object {
        private val logger: Logger = Logger.getLogger("CassandraClientURILogger")

        const val PREFIX: String = "jdbc:cassandra://"
    }
}
