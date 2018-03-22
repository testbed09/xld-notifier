package com.xebialabs.xldeploy.notifier;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Properties;

import com.xebialabs.deployit.engine.spi.event.*;
import de.xidra.automation.integration.util.PasswordEncrypter;
import de.xidra.automation.notification.deploymentengine.xldeploy.StepNotification;
import de.xidra.automation.notification.deploymentengine.xldeploy.XLDeployClient;
import de.xidra.automation.notification.ticketing.JiraConnector;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.xebialabs.deployit.engine.spi.execution.ExecutionStateListener;
import com.xebialabs.deployit.engine.spi.execution.StepExecutionStateEvent;
import com.xebialabs.deployit.engine.spi.execution.TaskExecutionStateEvent;

import nl.javadude.t2bus.Subscribe;

/**
 * This class is a plugin for XL Deploy that posts task status updates to a REST endpoint, such
 * as the endpoint included in the XebiaLabs lita bot.
 */
@DeployitEventListener
public class XldBotNotifier implements ExecutionStateListener {

	private static final Logger logger = LoggerFactory.getLogger(XldBotNotifier.class);

	private static String DEFAULT_BOT_URL = "http://localhost:9443";
	private String botURL = DEFAULT_BOT_URL;

	private transient XLDeployClient xlDeployClient;
	private transient JiraConnector jiraConnector;

	// will consume some memory, other option would be to make multiple calls the
	// rest interface to retrieve information
	private transient HashMap<String, LinkedList<StepNotification>> reports = new HashMap<>();

	private boolean featurePushStepInformationToPipeline = false;

	public XldBotNotifier()
	{
		String xldeployUrl = null;
		String xldeployUser = null;
		String xldeployPassword = null;

		String jiraUrl = null;
		String jiraUser = null;
		String jiraPassword = null;

		try
		{
			Properties props = new Properties();
			props.load(ClassLoader.getSystemResourceAsStream("xld-notifier.properties"));
			botURL = props.getProperty("pipeline.url", DEFAULT_BOT_URL);
			xldeployUrl = props.getProperty("xldeploy.url");
			xldeployUser = props.getProperty("xldeploy.user");
			xldeployPassword = props.getProperty("xldeploy.password");

			xldeployPassword = (new PasswordEncrypter()).decrypt(xldeployPassword);

			jiraUrl = props.getProperty("jira.url");
			jiraUser = props.getProperty("jira.user");
			jiraPassword = props.getProperty("jira.password");

			jiraPassword = (new PasswordEncrypter()).decrypt(jiraPassword);

			featurePushStepInformationToPipeline = Boolean.getBoolean(props.getProperty("feature.pushStepInformationToPipeline"));

		}
		catch(Exception e)
		{
			logger.warn("unable to read properties file");
		}


		xlDeployClient = new XLDeployClient(xldeployUser,xldeployPassword,xldeployUrl);
		jiraConnector = new JiraConnector(jiraUrl,jiraUser,jiraPassword);

		logger.debug("Using bot URL {}" , botURL);
	}

	@Subscribe
	public void log(TaskStartedEvent event) {
		postNotification(event.getTaskId(), "started");
	}

	@Subscribe
	public void log(TaskStoppedEvent event) {
		postNotification(event.getTaskId(), "stopped");
	}

	@Subscribe
	public void log(TaskCancelledEvent event) {
		postNotification(event.getTaskId(), "cancelled");
	}

	@Subscribe
	public void log(TaskAbortedEvent event) {
		postNotification(event.getTaskId(), "aborted");
	}

	@Subscribe
	public void log(TaskArchivedEvent event) {
		postNotification(event.getTaskId(), "archived");
	}

	@Subscribe
	public void log(TaskScheduledEvent event) {
		postNotification(event.getTaskId(), "scheduled");
	}


	private void postNotification(String taskId, String status) {
		try {
			HttpResponse<String> jsonResponse = Unirest.post(botURL + "/api/simplepipeline/updatedeployment/task/" + taskId + "/" + status)
					  .header("Content-type", "application/json")
					  .asString();
			if (jsonResponse.getStatus() != 200) {
				logger.warn("Failed to push event to bot, status code " + jsonResponse.getStatusText());
			}
		} catch (UnirestException e) {
			// fail silently
			logger.warn("Failed to push event to bot", e);
		}
	}

	//@Override
	public void stepStateChanged(StepExecutionStateEvent event)
	{

		Optional<String> ticketNo = xlDeployClient.getTicketIDForApplicationAndEnvironment(
				event.task().getMetadata().get("application"),
				event.task().getMetadata().get("environment_id"));

		StepNotification stepNotification = new StepNotification();
		stepNotification.setApplicationName(event.task().getMetadata().get("application"));
		stepNotification.setEnvironment(event.task().getMetadata().get("environment"));
		stepNotification.setVersion(event.task().getMetadata().get("version"));
		stepNotification.setLog(event.step().getLog());
		stepNotification.setStatus(event.currentState().toString());
		stepNotification.setTicketNo(ticketNo.orElse("Ticket number not present"));

		if(!reports.containsKey(event.task().getId()))
		{
			reports.put(event.task().getId(),new LinkedList<>());
		}
		LinkedList<StepNotification> l = reports.get(event.task().getId());
		l.add(stepNotification);
		reports.put(event.task().getId(),l);

		if(featurePushStepInformationToPipeline) {


			String pushUrl = botURL + "/api/simplepipeline/updatedeployment/task/" + event.task().getId() + "/step";
			logger.info("will push Notification to client {} information is {}", pushUrl, JSONObject.valueToString(stepNotification));

			try {
				HttpResponse<String> jsonResponse = Unirest.post(pushUrl)
						.header("Content-type", "application/json")
						.body((new JSONObject(stepNotification)).toString())
						.asString();
				if (jsonResponse.getStatus() != 200) {
					logger.warn("Failed to push event to bot, status code {}", jsonResponse.getStatusText());
				}
			} catch (UnirestException e) {
				// fail silently
				logger.warn("Failed to push event to bot", e);
			}
		}
	}

	//@Override
	public void taskStateChanged(TaskExecutionStateEvent event) {
		logger.info("Task {} state changed, {} pushing event to bot URL {}",event.task().getId(),
				event.currentState().toString(), botURL);

		String currentState = event.currentState().toString();
		String application = event.task().getMetadata().get("application");
		String environment = event.task().getMetadata().get("environment");
		String version = event.task().getMetadata().get("version");

		// deployment is executed, so collect all information and send it to the jira ticket
		if(currentState.equalsIgnoreCase("EXECUTED"))
		{
			//1. get the Jira Ticket
			//2. collect information
			//3. upload information
			Optional<String> ticketNo = xlDeployClient.getTicketIDForApplicationAndEnvironment(
					application,
					event.task().getMetadata().get("environment_id"));

			if(ticketNo.isPresent())
			{
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				try
				{
					out.write(reports.get(event.task().getId()).toString().getBytes());
					out.close();
					boolean result = jiraConnector.addAttachmentToIssue(ticketNo.get(),"InstallationReport_" + application+ "_"  +
							version + "_" + environment + ".txt",out.toByteArray());
					logger.info("{} updating ticket {} with InstallationReport was {}",ticketNo,ticketNo,result);
				}
				catch (IOException e)
				{
					logger.error("unable to update ticket",e);
				}
			}
			reports.remove(event.task().getId());
		}
		else
		{
			logger.debug("TASK STATE {} DOES NOT TRIGGER SOMETHING",currentState);
		}
		postNotification(event.task().getId(), event.currentState().toString().toLowerCase());
	}

	@Subscribe
	public void log(AuditableDeployitEvent event) {
		logger.info("[{}] - {} - {}", new Object[] { event.component, event.username, event.message });
	}

}
