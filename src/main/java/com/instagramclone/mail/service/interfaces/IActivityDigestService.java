package com.instagramclone.mail.service.interfaces;

public interface IActivityDigestService {

    /**
     * Executes the daily activity digest job across all opted-in users with bounded pagination,
     * distributed coordination, and 24-hour duplicate prevention.
     * @return Number of digest emails successfully dispatched.
     */
    int executeDailyActivityDigest();

    /**
     * Executes the weekly activity digest job across all opted-in users with bounded pagination,
     * distributed coordination, and 7-day duplicate prevention.
     * @return Number of digest emails successfully dispatched.
     */
    int executeWeeklyActivityDigest();
}
