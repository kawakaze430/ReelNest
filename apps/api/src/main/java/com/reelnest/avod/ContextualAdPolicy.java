package com.reelnest.avod;

/**
 * No decision is an ad playback instruction. Consent, current content approval,
 * country restrictions and non-child status must all be verified server-side.
 */
public final class ContextualAdPolicy {
    private ContextualAdPolicy() {}
    public static boolean previewAllowed(boolean optIn,boolean child,boolean paid,
                                         boolean freeTitle,boolean licensed,
                                         boolean approvedCampaign,boolean genreMatches) {
        return optIn && !child && !paid && freeTitle && licensed &&
               approvedCampaign && genreMatches;
    }
    public static boolean measureAllowed(boolean optIn,boolean child,boolean measurementOptIn,
                                         boolean decisionCurrentlyValid) {
        return optIn && !child && measurementOptIn && decisionCurrentlyValid;
    }
    public static boolean safeText(String value,int max) {
        if(value==null || value.strip().length()<2 || value.length()>max)return false;
        for(int i=0;i<value.length();i++) {
            char c=value.charAt(i);
            if(c=='<' || c=='>' || Character.isISOControl(c))return false;
        }
        return true;
    }
}
