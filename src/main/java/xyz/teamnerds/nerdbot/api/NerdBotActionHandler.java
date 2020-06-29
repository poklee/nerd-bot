package xyz.teamnerds.nerdbot.api;

public interface NerdBotActionHandler
{

    public void handleHelpAction(HelpAction action);

    public void handleKarmaReadAction(KarmaReadAction action);

    public void handleKarmaUpdateAction(KarmaUpdateAction action);

    public void handleKarmaReadRankingAction(KarmaReadRankingAction action);

}
