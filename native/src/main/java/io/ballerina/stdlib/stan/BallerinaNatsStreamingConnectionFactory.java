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
package io.ballerina.stdlib.stan;

import io.ballerina.runtime.api.TypeTags;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.utils.TypeUtils;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BDecimal;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.stdlib.crypto.nativeimpl.Decode;
import io.nats.client.Connection;
import io.nats.client.Nats;
import io.nats.streaming.Options;
import io.nats.streaming.StreamingConnection;
import io.nats.streaming.StreamingConnectionFactory;

import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

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

    public StreamingConnection createConnection() throws Exception {
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
                SSLContext sslContext = getSslContext(secureSocket);
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

    private static SSLContext getSslContext(BMap<BString, ?> secureSocket) throws Exception {
        // protocol
        String protocol = null;
        if (secureSocket.containsKey(Constants.PROTOCOL)) {
            @SuppressWarnings("unchecked")
            BMap<BString, Object> protocolRecord =
                    (BMap<BString, Object>) secureSocket.getMapValue(Constants.PROTOCOL);
            protocol = protocolRecord.getStringValue(Constants.PROTOCOL_NAME).getValue();
        }

        Object cert = secureSocket.get(Constants.CERT);
        @SuppressWarnings("unchecked")
        BMap<BString, BString> key = (BMap<BString, BString>) getBMapValueIfPresent(secureSocket, Constants.KEY);

        KeyManagerFactory kmf;
        TrustManagerFactory tmf;
        if (cert instanceof BString) {
            if (key != null) {
                if (key.containsKey(Constants.CERT_FILE)) {
                    BString certFile = key.get(Constants.CERT_FILE);
                    BString keyFile = key.get(Constants.KEY_FILE);
                    BString keyPassword = getBStringValueIfPresent(key, Constants.KEY_PASSWORD);
                    kmf = getKeyManagerFactory(certFile, keyFile, keyPassword);
                } else {
                    kmf = getKeyManagerFactory(key);
                }
                tmf = getTrustManagerFactory((BString) cert);
                return buildSslContext(kmf.getKeyManagers(), tmf.getTrustManagers(), protocol);
            } else {
                tmf = getTrustManagerFactory((BString) cert);
                return buildSslContext(null, tmf.getTrustManagers(), protocol);
            }
        }
        if (cert instanceof BMap) {
            BMap<BString, BString> trustStore = (BMap<BString, BString>) cert;
            if (key != null) {
                if (key.containsKey(Constants.CERT_FILE)) {
                    BString certFile = key.get(Constants.CERT_FILE);
                    BString keyFile = key.get(Constants.KEY_FILE);
                    BString keyPassword = getBStringValueIfPresent(key, Constants.KEY_PASSWORD);
                    kmf = getKeyManagerFactory(certFile, keyFile, keyPassword);
                } else {
                    kmf = getKeyManagerFactory(key);
                }
                tmf = getTrustManagerFactory(trustStore);
                return buildSslContext(kmf.getKeyManagers(), tmf.getTrustManagers(), protocol);
            } else {
                tmf = getTrustManagerFactory(trustStore);
                return buildSslContext(null, tmf.getTrustManagers(), protocol);
            }
        }
        return null;
    }

    private static TrustManagerFactory getTrustManagerFactory(BString cert) throws Exception {
        Object publicKeyMap = Decode.decodeRsaPublicKeyFromCertFile(cert);
        if (publicKeyMap instanceof BMap) {
            X509Certificate x509Certificate = (X509Certificate) ((BMap<BString, Object>) publicKeyMap).getNativeData(
                    Constants.NATIVE_DATA_PUBLIC_KEY_CERTIFICATE);
            KeyStore ts = KeyStore.getInstance(Constants.PKCS12);
            ts.load(null, "".toCharArray());
            ts.setCertificateEntry(UUID.randomUUID().toString(), x509Certificate);
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(ts);
            return tmf;
        } else {
            throw new Exception("Failed to get the public key from Crypto API. " +
                    ((BError) publicKeyMap).getErrorMessage().getValue());
        }
    }

    private static TrustManagerFactory getTrustManagerFactory(BMap<BString, BString> trustStore) throws Exception {
        BString trustStorePath = trustStore.getStringValue(Constants.KEY_STORE_PATH);
        BString trustStorePassword = trustStore.getStringValue(Constants.KEY_STORE_PASS);
        KeyStore ts = getKeyStore(trustStorePath, trustStorePassword);
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        tmf.init(ts);
        return tmf;
    }

    private static KeyManagerFactory getKeyManagerFactory(BMap<BString, BString> keyStore) throws Exception {
        BString keyStorePath = keyStore.getStringValue(Constants.KEY_STORE_PATH);
        BString keyStorePassword = keyStore.getStringValue(Constants.KEY_STORE_PASS);
        KeyStore ks = getKeyStore(keyStorePath, keyStorePassword);
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, keyStorePassword.getValue().toCharArray());
        return kmf;
    }

    private static KeyManagerFactory getKeyManagerFactory(BString certFile, BString keyFile, BString keyPassword)
            throws Exception {
        Object publicKey = Decode.decodeRsaPublicKeyFromCertFile(certFile);
        if (publicKey instanceof BMap) {
            X509Certificate publicCert = (X509Certificate) ((BMap<BString, Object>) publicKey).getNativeData(
                    Constants.NATIVE_DATA_PUBLIC_KEY_CERTIFICATE);
            Object privateKeyMap = Decode.decodeRsaPrivateKeyFromKeyFile(keyFile, keyPassword);
            if (privateKeyMap instanceof BMap) {
                PrivateKey privateKey = (PrivateKey) ((BMap<BString, Object>) privateKeyMap).getNativeData(
                        Constants.NATIVE_DATA_PRIVATE_KEY);
                KeyStore ks = KeyStore.getInstance(Constants.PKCS12);
                ks.load(null, "".toCharArray());
                ks.setKeyEntry(UUID.randomUUID().toString(), privateKey, "".toCharArray(),
                        new X509Certificate[]{publicCert});
                KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                kmf.init(ks, "".toCharArray());
                return kmf;
            } else {
                throw new Exception("Failed to get the private key from Crypto API. " +
                        ((BError) privateKeyMap).getErrorMessage().getValue());
            }
        } else {
            throw new Exception("Failed to get the public key from Crypto API. " +
                    ((BError) publicKey).getErrorMessage().getValue());
        }
    }

    private static KeyStore getKeyStore(BString path, BString password) throws Exception {
        try (FileInputStream is = new FileInputStream(path.getValue())) {
            char[] passphrase = password.getValue().toCharArray();
            KeyStore ks = KeyStore.getInstance(Constants.PKCS12);
            ks.load(is, passphrase);
            return ks;
        }
    }

    private static SSLContext buildSslContext(KeyManager[] keyManagers, TrustManager[] trustManagers,
                                              String protocol) throws Exception {
        SSLContext sslContext =
                SSLContext.getInstance(Objects.requireNonNullElse(protocol,
                        io.nats.client.Options.DEFAULT_SSL_PROTOCOL));
        sslContext.init(keyManagers, trustManagers, new SecureRandom());
        return sslContext;
    }

    private static BMap<BString, ?> getBMapValueIfPresent(BMap<BString, ?> config, BString key) {
        return config.containsKey(key) ? (BMap<BString, ?>) config.getMapValue(key) : null;
    }

    private static BString getBStringValueIfPresent(BMap<BString, ?> config, BString key) {
        return config.containsKey(key) ? config.getStringValue(key) : null;
    }
}
