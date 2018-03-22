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

package de.xidra.automation.notification.ticketing;

import de.xidra.automation.notification.BaseConnector;
import org.apache.http.HttpEntity;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;


/**
 * Class to connect to Jira and create/update tickets within the scope of processing a delivery
 */
public class JiraConnector extends BaseConnector
{
    private static final String ENDPOINT_ISSUE = "/rest/api/2/issue/";

    private static Logger log = LoggerFactory.getLogger(JiraConnector.class);

    public JiraConnector(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    private void addHeaderForCredentials(HttpPost httpPost) {
        httpPost.addHeader(BasicScheme.authenticate(
                new UsernamePasswordCredentials(user, password),
                "UTF-8", false));
    }

    public boolean addAttachmentToIssue(String issueKey, String fileName, byte file[]) throws IOException {

        try (CloseableHttpClient httpclient = createHttpClient())
        {

            HttpPost httppost = new HttpPost(url + ENDPOINT_ISSUE + issueKey + "/attachments");
            httppost.setHeader("X-Atlassian-Token", "no-check");

            addHeaderForCredentials(httppost);

            ContentBody contentBody = new ByteArrayBody(file, fileName);

            HttpEntity entity = MultipartEntityBuilder.create()
                    .addPart("file", contentBody)
                    .build();

            httppost.setEntity(entity);
            String message = "executing request " + httppost.getRequestLine();
            log.info("{} message from attaching attachment : {}",issueKey,message);

            CloseableHttpResponse response;

            response = httpclient.execute(httppost);
            return response.getStatusLine().getStatusCode() == 200;

        }
        catch (Exception e)
        {
            log.error("unable to add attachment to ticket",e);
        }
        return false;

    }

}
