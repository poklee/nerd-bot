package xyz.teamnerds.nerdbot.api;

public interface KarmaActionHandler
{

    public void handleHelpAction(KarmaHelpAction action);

    public void handleKarmaReadAction(KarmaReadAction action);

    public void handleKarmaUpdateAction(KarmaUpdateAction action);

    public void handleKarmaReadRankingAction(KarmaReadRankingAction action);

}
