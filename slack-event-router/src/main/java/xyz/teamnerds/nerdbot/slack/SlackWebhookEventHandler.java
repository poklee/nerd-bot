package xyz.teamnerds.nerdbot.slack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import com.slack.api.model.block.LayoutBlock;
import com.slack.api.model.block.RichTextBlock;
import com.slack.api.model.block.element.BlockElement;
import com.slack.api.model.block.element.RichTextElement;
import com.slack.api.model.block.element.RichTextSectionElement;

/**
 * Handles slack event payload
 * 
 * @author plee
 *
 */
public interface SlackWebhookEventHandler
{
    /**
     * Handle the payload, this method should return fast. Future improvements, we
     * should implement some sort of messaging queue and this method will put the
     * payload on the messaging queue
     * 
     * @param contentBody the json content
     */
    public void handlePayload(String contentBody);
    
    @Nonnull
    public static List<String> parseWordsFromLayoutBlocks(@Nonnull List<LayoutBlock> layoutBlocks)
    {
        // Parse message to build the user karma map
        List<String> results = new ArrayList<>();
        for (LayoutBlock layoutBlock : layoutBlocks)
        {
            if (layoutBlock instanceof RichTextBlock)
            {
                RichTextBlock richTextBlock = (RichTextBlock) layoutBlock;
                for (BlockElement blockElement : richTextBlock.getElements())
                {
                    if (blockElement instanceof RichTextSectionElement)
                    {
                        RichTextSectionElement richTextSectionElement = (RichTextSectionElement) blockElement;
                        for (RichTextElement richTextElement : richTextSectionElement.getElements())
                        {
                            if (richTextElement instanceof RichTextSectionElement.Text)
                            {
                                RichTextSectionElement.Text richTextSectionElementText = (RichTextSectionElement.Text) richTextElement;
                                String text = richTextSectionElementText.getText();
                                results.addAll(parseWordsFromText(text));
                            }
                        }
                    }
                }
            }
        }
        
        return results;
    }
    
    @Nonnull
    public static List<String> parseWordsFromText(@CheckForNull String text)
    {
        if (text == null)
        {
            return Collections.emptyList();
        }
        
        List<String> results = new ArrayList<>();
        String[] parts = text.split("\\W");
        for (String part : parts)
        {
            part = part.trim();
            if (part.length() > 0)
            {
                results.add(part);
            }
        }
        return results;
    }

}
