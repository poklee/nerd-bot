package xyz.teamnerds.nerdbot.dao;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import javax.annotation.Nonnull;

@Getter
@Builder
@ToString
@EqualsAndHashCode
public class UserKarma
{
    /**
     * The user id
     */
    @lombok.NonNull
    @Nonnull
    private String userId;

    /**
     * The karma for the particular user
     */
    private int karma;
}
