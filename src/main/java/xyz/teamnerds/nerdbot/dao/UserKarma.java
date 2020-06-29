package xyz.teamnerds.nerdbot.dao;

import javax.annotation.Nonnull;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

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
