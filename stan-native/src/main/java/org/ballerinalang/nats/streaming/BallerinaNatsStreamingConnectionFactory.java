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
import io.ballerina.runtime.api.values.BDecimal;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BString;
import io.nats.client.Connection;
import io.nats.client.Nats;
import io.nats.streaming.Options;
import io.nats.streaming.StreamingConnection;
import io.nats.streaming.StreamingConnectionFactory;
import org.ballerinalang.nats.Constants;
import org.ballerinalang.nats.Utils;

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

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

/**
 * Wraps {@link StreamingConnectionFactory}.
 */
public class BallerinaNatsStreamingConnectionFactory {
    private final BMap<BString, Object> streamingConfig;
    private final String url;
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

    public BallerinaNatsStreamingConnectionFactory(String url, String clusterId, String clientId,
                                                   BMap<BString, Object> streamingConfig) {
        this.streamingConfig = streamingConfig;
        this.url = url;
        this.clusterId = clusterId;
        this.clientId = clientId;
    }

    public StreamingConnection createConnection()
            throws IOException, InterruptedException, UnrecoverableKeyException, CertificateException,
                   NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        Options.Builder opts = new Options.Builder();
        opts.natsUrl(url);
        opts.clientId(clientId);
        opts.clusterId(clusterId);

        io.nats.client.Options.Builder natsOptions = new io.nats.client.Options.Builder();

        if (streamingConfig != null && TypeUtils.getType(streamingConfig).getTag() == TypeTags.RECORD_TYPE_TAG) {
            // Auth configs
            @SuppressWarnings("unchecked")
            Object authConfig = ((BMap) streamingConfig).getObjectValue(AUTH_CONFIG);
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
            // Secure socket configs
            BMap secureSocket = ((BMap) streamingConfig).getMapValue(Constants.CONNECTION_CONFIG_SECURE_SOCKET);
            if (secureSocket != null) {
                SSLContext sslContext = getSSLContext(secureSocket);
                natsOptions.sslContext(sslContext);
            }
            natsOptions.server(url);
            Connection natsConnection = Nats.connect(natsOptions.build());
            opts.discoverPrefix(streamingConfig.getStringValue(DISCOVERY_PREFIX).getValue());
            opts.connectWait(Duration.ofSeconds(((BDecimal) streamingConfig.get(CONNECTION_TIMEOUT)).intValue()));
            opts.pubAckWait(Duration.ofSeconds(((BDecimal) streamingConfig.get(ACK_TIMEOUT)).intValue()));
            opts.pingInterval(Duration.ofSeconds(((BDecimal) streamingConfig.get(PING_INTERVAL)).intValue()));
            opts.maxPubAcksInFlight(streamingConfig.getIntValue(MAX_PUB_ACKS_IN_FLIGHT).intValue());
            opts.natsConn(natsConnection);
        }
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
            throws IOException, CertificateException, NoSuchAlgorithmException, KeyStoreException,
                   UnrecoverableKeyException, KeyManagementException {
        // Keystore
        KeyManagerFactory keyManagerFactory = null;
        if (secureSocket.containsKey(Constants.CONNECTION_KEYSTORE)) {
            @SuppressWarnings("unchecked")
            BMap<BString, Object> cryptoKeyStore =
                    (BMap<BString, Object>) secureSocket.getMapValue(Constants.CONNECTION_KEYSTORE);
            char[] keyPassphrase = cryptoKeyStore.getStringValue(Constants.KEY_STORE_PASS).getValue().toCharArray();
            String keyFilePath = cryptoKeyStore.getStringValue(Constants.KEY_STORE_PATH).getValue();
            KeyStore keyStore = KeyStore.getInstance(Constants.KEY_STORE_TYPE);
            if (keyFilePath != null) {
                try (FileInputStream keyFileInputStream = new FileInputStream(keyFilePath)) {
                    keyStore.load(keyFileInputStream, keyPassphrase);
                }
            } else {
                throw Utils.createNatsError(Constants.ERROR_SETTING_UP_SECURED_CONNECTION +
                                                    "Keystore path doesn't exist.");
            }
            keyManagerFactory =
                    KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, keyPassphrase);
        }

        // Truststore
        @SuppressWarnings("unchecked")
        BMap<BString, Object> cryptoTrustStore =
                (BMap<BString, Object>) secureSocket.getMapValue(Constants.CONNECTION_TRUSTORE);
        KeyStore trustStore = KeyStore.getInstance(Constants.KEY_STORE_TYPE);
        char[] trustPassphrase = cryptoTrustStore.getStringValue(Constants.KEY_STORE_PASS).getValue()
                .toCharArray();
        String trustFilePath = cryptoTrustStore.getStringValue(Constants.KEY_STORE_PATH).getValue();
        if (trustFilePath != null) {
            try (FileInputStream trustFileInputStream = new FileInputStream(trustFilePath)) {
                trustStore.load(trustFileInputStream, trustPassphrase);
            }
        } else {
            throw Utils.createNatsError(Constants.ERROR_SETTING_UP_SECURED_CONNECTION
                                                + "truststore path doesn't exist.");
        }
        TrustManagerFactory trustManagerFactory =
                TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(trustStore);

        // protocol
        SSLContext sslContext;
        if (secureSocket.containsKey(Constants.CONNECTION_PROTOCOL)) {
            @SuppressWarnings("unchecked")
            BMap<BString, Object> protocolRecord =
                    (BMap<BString, Object>) secureSocket.getMapValue(Constants.CONNECTION_PROTOCOL);
            String protocol = protocolRecord.getStringValue(Constants.CONNECTION_PROTOCOL_NAME).getValue();
            sslContext = SSLContext.getInstance(protocol);
        } else {
            sslContext = SSLContext.getDefault();
        }
        sslContext.init(keyManagerFactory != null ? keyManagerFactory.getKeyManagers() : null,
                        trustManagerFactory.getTrustManagers(), new SecureRandom());
        return sslContext;
    }
}
