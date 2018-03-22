/*
 *  * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 *
 *       THIS MATERIAL IS PROPRIETARY  TO  EDUARD VAN DEN BONGARD
 *       AND IS  NOT TO BE REPRODUCED, USED OR  DISCLOSED  EXCEPT
 *       IN ACCORDANCE  WITH  PROGRAM  LICENSE  OR  UPON  WRITTEN
 *       AUTHORIZATION  OF  EDUARD VAN DEN BONGARD, AM STIRKENBEND 20
 *       41352 KORSCHENBROICH, GERMANY.
 *
 *       COPYRIGHT (C) 2013-17 EDUARD VAN DEN BONGARD
 *
 *  * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 */

package de.xidra.automation.notification;

import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;

import javax.net.ssl.SSLContext;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

/**
 * Base class for connectors using HTTPS to connect to REST Services
 */
public abstract class BaseConnector
{

    protected String url;
    protected String user;
    protected String password;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    protected CloseableHttpClient createHttpClient() throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException {
        SSLContext sslContext = new  SSLContextBuilder()
                .loadTrustMaterial(null, (certificate, authType) -> true).build();


        return HttpClients.custom()
                .setSSLContext(sslContext)
                .setSSLHostnameVerifier(new NoopHostnameVerifier())
                .build();
    }

}
