package xyz.teamnerds.nerdbot.slack;

//@Configuration
public class SlackConfiguration
{

    private String signingSecret = System.getenv("SLACK_BOT_SIGNING_SECRET");
    private String clientId = System.getenv("SLACK_BOT_CLIENT_ID");
    private String token = System.getenv("SLACK_BOT_TOKEN");
    private String appId;

    public String getAppId()
    {
        return appId;
    }

    public String getClientId()
    {
        return clientId;
    }

    public String getSigningSecret()
    {
        return signingSecret;
    }

    public String getToken()
    {
        return token;
    }
}
