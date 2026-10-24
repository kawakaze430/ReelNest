package com.reelnest.billing;

import java.time.Instant;

/** TVOD time-window policy only: NEVER creates rent payment or playback rights. */
public final class RentalWindowPolicy {
    private RentalWindowPolicy() {}
    public static boolean validWindow(Instant paidAt,Instant firstPlay,
                                     Instant now,Instant licensedUntil,
                                     long rentalHours,long watchHours) {
        if(paidAt==null || now==null || licensedUntil==null ||
           rentalHours<1 || rentalHours>720 || watchHours<1 || watchHours>168 ||
           now.isBefore(paidAt))return false;
        try {
            Instant rentalEnd=paidAt.plusSeconds(Math.multiplyExact(rentalHours,3600));
            if(!now.isBefore(rentalEnd) || !now.isBefore(licensedUntil))return false;
            return firstPlay==null || (!firstPlay.isBefore(paidAt) &&
                !firstPlay.isAfter(now) &&
                now.isBefore(firstPlay.plusSeconds(Math.multiplyExact(watchHours,3600))));
        }catch(RuntimeException invalid){return false;}
    }
}
