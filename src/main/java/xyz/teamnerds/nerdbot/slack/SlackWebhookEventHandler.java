package xyz.teamnerds.nerdbot.slack;

public interface SlackWebhookEventHandler
{
	
	
	/**
	 * Handle the payload, this method should return fast.  
	 * Future improvements, we should implement some sort of messaging queue
	 * and this method will put the payload on the messaging queue
	 * @param payload
	 */
	public void handlePayload(String contentBody);

}
