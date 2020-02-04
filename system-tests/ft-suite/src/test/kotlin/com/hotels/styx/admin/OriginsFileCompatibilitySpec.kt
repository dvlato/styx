/*
  Copyright (C) 2013-2020 Expedia Inc.

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
 */
package com.hotels.styx.admin

import com.github.tomakehurst.wiremock.client.WireMock
import com.hotels.styx.NettyExecutor
import com.hotels.styx.api.HttpHeaderNames.HOST
import com.hotels.styx.api.HttpRequest.get
import com.hotels.styx.api.HttpResponseStatus.BAD_GATEWAY
import com.hotels.styx.api.HttpResponseStatus.OK
import com.hotels.styx.client.StyxHttpClient
import com.hotels.styx.server.HttpConnectorConfig
import com.hotels.styx.server.HttpsConnectorConfig
import com.hotels.styx.servers.MockOriginServer
import com.hotels.styx.support.ResourcePaths
import com.hotels.styx.support.StyxServerProvider
import com.hotels.styx.support.adminHostHeader
import com.hotels.styx.support.proxyHttpHostHeader
import com.hotels.styx.support.wait
import io.kotlintest.Spec
import io.kotlintest.eventually
import io.kotlintest.seconds
import io.kotlintest.shouldBe
import io.kotlintest.specs.FunSpec
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.charset.StandardCharsets.UTF_8

class OriginsFileCompatibilitySpec : FunSpec() {
    val tempDir = createTempDir(suffix = "-${this.javaClass.simpleName}")
    val originsFile = File(tempDir, "origins.yml")
    val LOGGER = LoggerFactory.getLogger(OriginsFileCompatibilitySpec::class.java)

    val styxServer = StyxServerProvider(
            defaultConfig = """
                ---
                proxy:
                  connectors:
                    http:
                      port: 0
        
                admin:
                  connectors:
                    http:
                      port: 0

                request-logging:
                  inbound:
                    enabled: true
                  outbound:
                    enabled: true

                providers:
                  originsFileLoader:
                    type: YamlFileConfigurationService
                    config:
                      originsFile: ${originsFile.absolutePath}
                      ingressObject: pathPrefixRouter
                      monitor: True
                      pollInterval: PT0.1S 

                httpPipeline: pathPrefixRouter
                """.trimIndent(),
            defaultLoggingConfig = ResourcePaths.fixturesHome(
                    OriginsFileCompatibilitySpec::class.java,
                    "/conf/logback/logback.xml")
                    .toAbsolutePath())

    init {

        context("Origins configuration changes") {
            writeOrigins("""
                - id: appA
                  path: "/"
                  origins:
                  - { id: "appA-01", host: "localhost:${mockServerA01.port()}" } 
            """.trimIndent())
            styxServer.restart()

            test("TLSSettings modifications") {
                writeOrigins("""
                    - id: appTls
                      path: "/"
                      tlsSettings:
                        trustAllCerts: true
                        sslProvider: JDK
                        protocols:
                          - TLSv1.1
                      origins:
                      - { id: "appTls-01", host: "localhost:${mockTlsv12Server.port()}" } 
                    """.trimIndent())
                eventually(2.seconds, AssertionError::class.java) {
                    client.send(get("/11")
                            .header(HOST, styxServer().proxyHttpHostHeader())
                            .build())
                            .wait().let {
                                it!!.status() shouldBe BAD_GATEWAY
                            }
                }

                writeOrigins("""
                    - id: appTls
                      path: "/"
                      tlsSettings:
                        trustAllCerts: true
                        sslProvider: JDK
                        protocols:
                          - TLSv1.2
                      origins:
                      - { id: "appTls-01", host: "localhost:${mockTlsv12Server.port()}" } 
                    """.trimIndent())

                eventually(2.seconds, AssertionError::class.java) {
                    client.send(get("/12")
                            .header(HOST, styxServer().proxyHttpHostHeader())
                            .build())
                            .wait().let {
                                it!!.status() shouldBe OK
                                it.bodyAs(UTF_8) shouldBe "appTls-01"
                            }
                }
            }
        }

    }

    internal fun writeOrigins(text: String, debug: Boolean = true) {
        originsFile.writeText(text)
        if (debug) {
            LOGGER.info("new origins file: \n${originsFile.readText()}")
        }
    }

    fun dumpObjectDatabase() = client.send(get("/admin/routing/objects")
            .header(HOST, styxServer().adminHostHeader())
            .build())
            .wait()
            .bodyAs(UTF_8)!!

    val mockServerA01 = MockOriginServer.create("appA", "appA-01", 0, HttpConnectorConfig(0))
            .start()
            .stub(WireMock.get(WireMock.urlMatching("/.*")), WireMock.aResponse()
                    .withStatus(200)
                    .withBody("appA-01"))

    val mockServerA02 = MockOriginServer.create("appA", "appA-02", 0, HttpConnectorConfig(0))
            .start()
            .stub(WireMock.get(WireMock.urlMatching("/.*")), WireMock.aResponse()
                    .withStatus(200)
                    .withBody("appA-02"))

    val mockServerB01 = MockOriginServer.create("appB", "appB-01", 0, HttpConnectorConfig(0))
            .start()
            .stub(WireMock.get(WireMock.urlMatching("/.*")), WireMock.aResponse()
                    .withStatus(200)
                    .withBody("appB-01"))

    val mockServerC01 = MockOriginServer.create("appC", "appC-01", 0, HttpConnectorConfig(0))
            .start()
            .stub(WireMock.get(WireMock.urlMatching("/.*")), WireMock.aResponse()
                    .withStatus(200)
                    .withBody("appC-01"))

    val mockTlsv12Server = MockOriginServer.create("appTls", "appTls-01", 0,
            HttpsConnectorConfig.Builder()
                    .port(0)
                    .sslProvider("JDK")
                    .protocols("TLSv1.2")
                    .build())
            .start()
            .stub(WireMock.get(WireMock.urlMatching("/.*")), WireMock.aResponse()
                    .withStatus(200)
                    .withBody("appTls-01"))

    override fun afterSpec(spec: Spec) {
        styxServer.stop()
        tempDir.deleteRecursively()

        mockServerA01.stop()
        mockServerA02.stop()
        mockServerB01.stop()
        mockServerC01.stop()
        mockTlsv12Server.stop()
    }
}

private val client: StyxHttpClient = StyxHttpClient.Builder()
        .executor(NettyExecutor.create("styx-client", 0))
        .build()
