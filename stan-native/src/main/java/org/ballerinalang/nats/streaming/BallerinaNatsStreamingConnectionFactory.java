/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.ballerinalang.nats.streaming;

import io.ballerina.runtime.api.TypeTags;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.utils.TypeUtils;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BDecimal;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BString;
import io.nats.client.Connection;
import io.nats.client.Nats;
import io.nats.streaming.Options;
import io.nats.streaming.StreamingConnection;
import io.nats.streaming.StreamingConnectionFactory;
import org.ballerinalang.nats.Constants;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.time.Duration;
import java.util.Objects;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

/**
 * Wraps {@link StreamingConnectionFactory}.
 */
public class BallerinaNatsStreamingConnectionFactory {
    private final BMap<BString, Object> streamingConfig;
    private final Object url;
    private final String clusterId;
    private final String clientId;

    private static final BString AUTH_CONFIG = StringUtils.fromString("auth");
    private static final BString USERNAME = StringUtils.fromString("username");
    private static final BString PASSWORD = StringUtils.fromString("password");
    private static final BString TOKEN = StringUtils.fromString("token");
    private static final BString ACK_TIMEOUT = StringUtils.fromString("ackTimeout");
    private static final BString CONNECTION_TIMEOUT = StringUtils.fromString("connectionTimeout");
    private static final BString MAX_PUB_ACKS_IN_FLIGHT = StringUtils.fromString("maxPubAcksInFlight");
    private static final BString DISCOVERY_PREFIX = StringUtils.fromString("discoverPrefix");
    private static final BString PING_INTERVAL = StringUtils.fromString("pingInterval");

    public BallerinaNatsStreamingConnectionFactory(Object url, String clusterId, String clientId,
                                                   BMap<BString, Object> streamingConfig) {
        this.streamingConfig = streamingConfig;
        this.url = url;
        this.clusterId = clusterId;
        this.clientId = clientId;
    }

    public StreamingConnection createConnection()
            throws IOException, InterruptedException, UnrecoverableKeyException, CertificateException,
                   NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        // stan streaming options
        Options.Builder opts = new Options.Builder();
        // underlying nats connection options
        io.nats.client.Options.Builder natsOptions = new io.nats.client.Options.Builder();
        if (TypeUtils.getType(url).getTag() == TypeTags.ARRAY_TAG) {
            // if string[]
            String[] serverUrls = ((BArray) url).getStringArray();
            natsOptions.servers(serverUrls);
        } else {
            // if string
            String serverUrl = ((BString) url).getValue();
            natsOptions.server(serverUrl);
        }
        opts.clientId(clientId);
        opts.clusterId(clusterId);

        // other configs
        if (streamingConfig != null && TypeUtils.getType(streamingConfig).getTag() == TypeTags.RECORD_TYPE_TAG) {
            // Auth configs
            if (streamingConfig.containsKey(AUTH_CONFIG)) {
                @SuppressWarnings("unchecked")
                Object authConfig = streamingConfig.getMapValue(AUTH_CONFIG);
                //Object authConfig2 = ((BMap) streamingConfig).getObjectValue(AUTH_CONFIG);
                if (TypeUtils.getType(authConfig).getTag() == TypeTags.RECORD_TYPE_TAG) {
                    if (((BMap) authConfig).containsKey(USERNAME) && ((BMap) authConfig).containsKey(PASSWORD)) {
                        // Credentials based auth
                        natsOptions.userInfo(((BMap) authConfig).getStringValue(USERNAME).getValue().toCharArray(),
                                ((BMap) authConfig).getStringValue(PASSWORD).getValue().toCharArray());
                    } else if (((BMap) authConfig).containsKey(TOKEN)) {
                        // Token based auth
                        natsOptions.token(((BMap) authConfig).getStringValue(TOKEN).getValue().toCharArray());
                    }
                }
            }
            // Secure socket configs
            BMap secureSocket = ((BMap) streamingConfig).getMapValue(Constants.CONNECTION_CONFIG_SECURE_SOCKET);
            if (secureSocket != null) {
                SSLContext sslContext = getSSLContext(secureSocket);
                natsOptions.sslContext(sslContext);
            }
            opts.discoverPrefix(streamingConfig.getStringValue(DISCOVERY_PREFIX).getValue());
            opts.connectWait(Duration.ofSeconds(((BDecimal) streamingConfig.get(CONNECTION_TIMEOUT)).intValue()));
            opts.pubAckWait(Duration.ofSeconds(((BDecimal) streamingConfig.get(ACK_TIMEOUT)).intValue()));
            opts.pingInterval(Duration.ofSeconds(((BDecimal) streamingConfig.get(PING_INTERVAL)).intValue()));
            opts.maxPubAcksInFlight(streamingConfig.getIntValue(MAX_PUB_ACKS_IN_FLIGHT).intValue());
        }

        Connection natsConnection = Nats.connect(natsOptions.build());
        // set the nats connection manually
        opts.natsConn(natsConnection);

        StreamingConnectionFactory streamingConnectionFactory = new StreamingConnectionFactory(opts.build());
        return streamingConnectionFactory.createConnection();
    }

    /**
     * Creates and retrieves the SSLContext from socket configuration.
     *
     * @param secureSocket secureSocket record.
     * @return Initialized SSLContext.
     */
    private static SSLContext getSSLContext(BMap<BString, Object> secureSocket)
            throws IOException, CertificateException, KeyStoreException,
            UnrecoverableKeyException, KeyManagementException, NoSuchAlgorithmException {
        // Keystore
        String keyFilePath = null;
        char[] keyPassphrase = null;
        char[] trustPassphrase;
        String trustFilePath;
        if (secureSocket.containsKey(Constants.CONNECTION_KEYSTORE)) {
            @SuppressWarnings("unchecked")
            BMap<BString, Object> cryptoKeyStore =
                    (BMap<BString, Object>) secureSocket.getMapValue(Constants.CONNECTION_KEYSTORE);
            keyPassphrase = cryptoKeyStore.getStringValue(Constants.KEY_STORE_PASS).getValue().toCharArray();
            keyFilePath = cryptoKeyStore.getStringValue(Constants.KEY_STORE_PATH).getValue();
        }

        // Truststore
        @SuppressWarnings("unchecked")
        BMap<BString, Object> cryptoTrustStore =
                (BMap<BString, Object>) secureSocket.getMapValue(Constants.CONNECTION_TRUSTORE);
        trustPassphrase = cryptoTrustStore.getStringValue(Constants.KEY_STORE_PASS).getValue()
                .toCharArray();
        trustFilePath = cryptoTrustStore.getStringValue(Constants.KEY_STORE_PATH).getValue();

        // protocol
        String protocol = null;
        if (secureSocket.containsKey(Constants.CONNECTION_PROTOCOL)) {
            @SuppressWarnings("unchecked")
            BMap<BString, Object> protocolRecord =
                    (BMap<BString, Object>) secureSocket.getMapValue(Constants.CONNECTION_PROTOCOL);
            protocol = protocolRecord.getStringValue(Constants.CONNECTION_PROTOCOL_NAME).getValue();
        }
        return createSSLContext(trustFilePath, trustPassphrase, keyFilePath, keyPassphrase, protocol);
    }

    public static KeyStore loadKeystore(String path, char[] pass) throws KeyStoreException, IOException,
            CertificateException, NoSuchAlgorithmException {
        KeyStore store = KeyStore.getInstance(Constants.KEY_STORE_TYPE);

        try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(path))) {
            store.load(in, pass);
        }
        return store;
    }

    public static KeyManager[] createTestKeyManagers(String keyStorePath, char[] keyStorePass)
            throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException,
            UnrecoverableKeyException {
        KeyStore store = loadKeystore(keyStorePath, keyStorePass);
        KeyManagerFactory factory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        factory.init(store, keyStorePass);
        return factory.getKeyManagers();
    }

    public static TrustManager[] createTestTrustManagers(String trustStorePath, char[] trustStorePass)
            throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException {
        KeyStore store = loadKeystore(trustStorePath, trustStorePass);
        TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        factory.init(store);
        return factory.getTrustManagers();
    }

    public static SSLContext createSSLContext(String trustStorePath, char[] trustStorePass, String keyStorePath,
                                              char[] keyStorePass, String protocol)
            throws UnrecoverableKeyException, CertificateException, KeyStoreException, IOException,
            NoSuchAlgorithmException, KeyManagementException {

        SSLContext ctx =
                SSLContext.getInstance(Objects.requireNonNullElse(protocol,
                        io.nats.client.Options.DEFAULT_SSL_PROTOCOL));
        ctx.init(keyStorePath != null ? createTestKeyManagers(keyStorePath, keyStorePass) : null,
                createTestTrustManagers(trustStorePath, trustStorePass), new SecureRandom());
        return ctx;
    }
}
