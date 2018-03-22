package de.xidra.automation.notification.deploymentengine.xldeploy;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Optional;


/**
 * Models the connection to XLDeploy
 */
public class XLDeployClient implements Serializable
{
    private final transient Logger logger = LoggerFactory.getLogger(getClass());

    private String user;
    private String password;
    private String url;

    public XLDeployClient(String user, String password, String url) {
        this.user = user;
        this.password = password;
        this.url = url;
    }

    public Optional<String> getTicketIDForApplicationAndEnvironment(String application, String environment)
    {
        logger.info("getting TicketNo for {} and {}",application,environment);
        String ticketNo = null;

        try
        {
            HttpResponse<String> jsonResponse = Unirest.get(url +"/deployit/repository/ci/" + environment)
                    .header("Content-type", "application/json")
                    .basicAuth(user, password)
                    .asString();
            if (jsonResponse.getStatus() != 200) {
                logger.debug("Failed to push event to bot, status code " + jsonResponse.getStatusText());
            }
            String info = jsonResponse.getBody();
            logger.info(info);
            info = info.substring(info.indexOf("<jiraTicket>") + "<jiraTicket>".length(),info.indexOf("</jiraTicket>"));
            ticketNo = info;
            logger.info("extracted ticketNo {} ",ticketNo);
        } catch (Exception e) {
            // fail silently
            logger.error("Failed to extract ticketNo", e);
        }
        return Optional.ofNullable(ticketNo);
    }

    public String retrieveTaskInformation(String taskId)
    {
        String taskInformation = null;

        logger.info("retrieving information for task {}",taskId);
        try {
            HttpResponse<String> xmlResponse = Unirest.get(url +"/deployit/tasks/v2/" + taskId)
                    .header("Content-type", "application/json")
                    .basicAuth(user, password)
                    .asString();
            if (xmlResponse.getStatus() != 200) {
                logger.warn("Failed to retrieve information for task {} from XLDeploy, status code {}",taskId,  xmlResponse.getStatusText());
            }
            taskInformation = xmlResponse.getBody();
        } catch (Exception e) {
            // fail silently
            logger.error("Failed to extract taskInformation", e);
        }
        return taskInformation;
    }

}
