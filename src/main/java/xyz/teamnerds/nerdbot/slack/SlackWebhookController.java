package xyz.teamnerds.nerdbot.slack;

import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
public class SlackWebhookController {

	private SlackConfiguration slackConfiguration = new SlackConfiguration();
	
	@Autowired
	private SlackWebhookEventHandler slackWebhookEventHandler;
	

	
	private Logger LOGGER = LoggerFactory.getLogger(SlackWebhookController.class);

	@PostMapping(path = "/slack", 
			consumes = "application/json", 
			produces = "text/plain")
	public String post(
			@RequestHeader(name = "X-Slack-Signature") String slackSignature,
			@RequestHeader(name = "X-Slack-Request-Timestamp") String slackRequestTimestampString,
			HttpEntity<String> httpEntity)
	{
		LOGGER.debug("SlackWebhook POST");
		LOGGER.debug("X-Slack-Signature: " + slackSignature);
		LOGGER.debug("X-Slack-Request-Timestamp: " + slackRequestTimestampString);
		
		String contentBody = httpEntity.getBody();
		LOGGER.debug("contentBody=" + contentBody);
		
		String generatedSlackSignature = generateSlackSignature(slackRequestTimestampString, contentBody);
		if (!generatedSlackSignature.equals(slackSignature))
		{
			// This is a bad signature, that means this request probably did not come from slack
			LOGGER.warn("Bad signature");
			throw new ResponseStatusException(HttpStatus.FORBIDDEN);
		}

		// Parse the json element now
		ObjectMapper objectMapper = new ObjectMapper();
		JsonNode rootNode = null;
		
		try
		{
			rootNode = objectMapper.readTree(contentBody);
		}
		catch (Exception ex)
		{
			LOGGER.warn("Failed to parse json payload", ex);
		}
		
		if (rootNode == null || !rootNode.isObject())
		{
			LOGGER.warn("POST doesn't contain json object?");
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
		}
		
		JsonNode typeElement = rootNode.get("type");
		if (typeElement == null || !typeElement.isTextual())
		{
			LOGGER.warn("Expected json /type to be a string");
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
		}
		
		// Handle verification event
		String typeElementString = typeElement.asText();
		if (typeElementString.equals("url_verification"))
		{
			return rootNode.get("challenge").asText();
		}
		

		try
		{
			slackWebhookEventHandler.handlePayload(contentBody);
		}
		catch (Exception ex)
		{
			LOGGER.warn("Failed to handle payload.  Json=" + contentBody, ex);
		}
		return "";
	}
	
	@GetMapping(path = "/")
	public String get()
	{
		LOGGER.debug("SlackWebhook GET");
		return "hello";
	}
	
	private String generateSlackSignature(String slackRequestTimestampString, String contentBody)
	{
		String slackSigningSecret = slackConfiguration.getSigningSecret();
		String signingAlgorithm = "HmacSHA256";
		try
		{
			// Create the secret key
			SecretKeySpec secret_key = new SecretKeySpec(slackSigningSecret.getBytes(), signingAlgorithm);
			
			// Init the hash algorithm
			Mac sha256_HMAC = Mac.getInstance(signingAlgorithm);
			sha256_HMAC.init(secret_key);
			
			String signingVersion = "v0";
			String signingMessage = signingVersion + ":" + slackRequestTimestampString + ":" + contentBody;

			byte[] signatureAsBytes = sha256_HMAC.doFinal(signingMessage.getBytes());
			String signatureAsHexString = Hex.encodeHexString(signatureAsBytes);
			
			return signingVersion + "=" + signatureAsHexString;
		}
		catch (NoSuchAlgorithmException ex)
		{
			LOGGER.error("JVM does not support signature algorithm " + signingAlgorithm, ex);
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
		}
		catch (Exception ex)
		{
			LOGGER.error("Failed to generate signature for request", ex);
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

}
