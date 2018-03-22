package de.xidra.automation.notification.deploymentengine.xldeploy;

/**
 * This class enacapsulates the notification to be sent.
 */
public class StepNotification
{
    private String applicationName;
    private String version;
    private String environment;
    private String status;
    private String log;
    private String ticketNo;

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environemnt) {
        this.environment = environemnt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getLog() {
        return log;
    }

    public void setLog(String log) {
        this.log = log;
    }

    public String getTicketNo() {
        return ticketNo;
    }

    public void setTicketNo(String ticketNo) {
        this.ticketNo = ticketNo;
    }

    @Override
    public String toString() {
        return "StepNotification{" +
                "applicationName='" + applicationName + '\'' +
                ", version='" + version + '\'' +
                ", environment='" + environment + '\'' +
                ", status='" + status + '\'' +
                ", log='" + log + '\'' +
                ", ticketNo='" + ticketNo + '\'' +
                '}';
    }
}
