package com.mediafire.sdk.token;

import com.mediafire.sdk.config.MFConfiguration;

public final class MFSessionToken extends MFToken {
    private static final String TAG = MFSessionToken.class.getCanonicalName();
    private final String time;
    private String secretKey;
    private final String pkey;
    private final String ekey;

    public MFSessionToken(String tokenString, String secretKey, String time, String pkey, String ekey) {
        super(tokenString);
        this.secretKey = secretKey;
        this.time = time;
        this.pkey = pkey;
        this.ekey = ekey;
    }

    /**
     * gets the time value for this Token.
     * @return the time value for this Token.
     */
    public String getTime() {
        return time;
    }

    /**
     * gets the secret key for this Token.
     * @return the secret key value for this Token.
     */
    public String getSecretKey() {
        return secretKey;
    }

    /**
     * gets the pkey value for this Token.
     * @return the pkey for this Token
     */
    public String getPkey() {
        return pkey;
    }

    /**
     * gets the ekey value for this Token.
     * @return the ekey for this Token.
     */
    public String getEkey() {
        return ekey;
    }

    /**
     * updates a session token using MediaFire calculation.
     */
    public void updateSessionToken() {
        MFConfiguration.getStaticMFLogger().v(TAG, "updateSessionToken()");
        MFConfiguration.getStaticMFLogger().v(TAG, "original secret key: " + secretKey);
        long newKey = Long.valueOf(secretKey) * 16807;
        MFConfiguration.getStaticMFLogger().v(TAG, "new secret key: " + newKey);
        newKey = newKey % 2147483647;
        MFConfiguration.getStaticMFLogger().v(TAG, "new secret key % 2147483647: " + newKey);
        String newSecretKey = String.valueOf(newKey);
        MFConfiguration.getStaticMFLogger().v(TAG, "string value of new key % 2147483647: " + newKey);
        this.secretKey = newSecretKey;
    }

    @Override
    public String toString() {
        return "MFSessionToken token [" + tokenString + "],  secret key [" + secretKey + "], time [" + time + "],pkey [" + pkey + "], ekey [" + ekey + "]";
    }
}
