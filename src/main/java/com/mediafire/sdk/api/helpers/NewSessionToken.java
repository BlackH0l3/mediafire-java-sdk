package com.mediafire.sdk.api.helpers;

import com.mediafire.sdk.api.responses.user.GetSessionTokenResponse;
import com.mediafire.sdk.config.IDeveloperCredentials;
import com.mediafire.sdk.config.ITokenManager;
import com.mediafire.sdk.config.IUserCredentials;
import com.mediafire.sdk.http.Request;
import com.mediafire.sdk.http.Response;
import com.mediafire.sdk.token.SessionToken;

/**
 * Created by Chris on 11/9/2014.
 * BaseClientHelper used with ApiClient to get new session tokens.
 */
public class NewSessionToken extends Instructions {
    private IUserCredentials mUserCredentials;
    private IDeveloperCredentials mDeveloperCredentials;
    private ITokenManager mSessionITokenManagerInterface;

    public NewSessionToken(IUserCredentials userCredentials, IDeveloperCredentials developerCredentials, ITokenManager sessionITokenManagerInterface) {
        super();
        mUserCredentials = userCredentials;
        mDeveloperCredentials = developerCredentials;
        mSessionITokenManagerInterface = sessionITokenManagerInterface;
    }
    
    @Override
    public void borrowToken(Request request) {
        // no token needs to be borrowed for new session tokens
        if (debugging()) {
            System.out.println(getClass() + " - borrowToken");
        }
    }

    @Override
    public void addSignatureToRequestParameters(Request request) {
        if (debugging()) {
            System.out.println(getClass() + " - addSignatureToRequestParameters");
        }
        addRequiredParametersForNewSessionToken(request);
        String signature = makeSignatureForNewSessionToken();
        request.addSignature(signature);
    }

    @Override
    public void returnToken(Response response, Request request) {
        if (debugging()) {
            System.out.println(getClass() + " - returnToken");
        }
        GetSessionTokenResponse newSessionTokenResponse = getResponseObject(response, GetSessionTokenResponse.class);
        SessionToken newSessionToken = createNewSessionToken(newSessionTokenResponse);
        if (newSessionToken != null) {
            mSessionITokenManagerInterface.give(newSessionToken);
        } else {
            if (debugging()) {
                System.out.println(getClass() + " - session token is null, not returning");
            }
        }
    }

    private String makeSignatureForNewSessionToken() {
        if (debugging()) {
            System.out.println(getClass() + " - makeSignatureForNewSessionToken");
        }
        // email + password + app id + api key
        // fb access token + app id + api key
        // tw oauth token + tw oauth token secret + app id + api key
        String userInfoPortionOfHashTarget = mUserCredentials.getCredentialsString();

        // apiKey is not required, but may be passed into the MFConfiguration object
        // Note: If the app does not have the "Require Secret Key" option checked,
        // then the API key may be omitted from the signature.
        // However, this should only be done when sufficient domain and/or network restrictions are in place.
        String devInfoPortionOfHashTarget = mDeveloperCredentials.getApplicationId();
        if (mDeveloperCredentials.requiresSecretKey()) {
            devInfoPortionOfHashTarget += mDeveloperCredentials.getApiKey();
        } else {
            if (debugging()) {
                System.out.println(getClass() + " - addRequiredParametersForNewSessionToken, does not require secret key");
            }
        }

        String hashTarget = userInfoPortionOfHashTarget + devInfoPortionOfHashTarget;

        String signature = hashString(hashTarget, "SHA-1");
        return signature;
    }

    private void addRequiredParametersForNewSessionToken(Request request) {
        IUserCredentials.Credentials credentials = mUserCredentials.getCredentials();

        if (debugging()) {
            System.out.println(getClass() + " - addRequiredParametersForNewSessionToken, credentials class " + credentials.getClass());
        }

        if (credentials instanceof IUserCredentials.Ekey) {
            request.addQueryParameter("ekey", ((IUserCredentials.Ekey) credentials).getEkey());
            request.addQueryParameter("password", ((IUserCredentials.Ekey) credentials).getPassword());
        } else if (credentials instanceof IUserCredentials.Email) {
            request.addQueryParameter("email", ((IUserCredentials.Email) credentials).getEmail());
            request.addQueryParameter("password", ((IUserCredentials.Email) credentials).getPassword());
        } else if (credentials instanceof IUserCredentials.Facebook) {
            request.addQueryParameter("fb_access_token", ((IUserCredentials.Facebook) credentials).getFacebookAccessToken());
        } else if (credentials instanceof IUserCredentials.Twitter) {
            request.addQueryParameter("tw_oauth_token", ((IUserCredentials.Twitter) credentials).getOauthToken());
            request.addQueryParameter("tw_oauth_token_secret", ((IUserCredentials.Twitter) credentials).getTokenSecret());
        } else {

        }

        request.addQueryParameter("application_id", mDeveloperCredentials.getApplicationId());
    }

    /**
     * Creates a SessionToken Object from a GetSessionTokenResponse
     * @param getSessionTokenResponse the response to create a SessionToken from
     * @return a new SessionToken Object
     */
    private SessionToken createNewSessionToken(GetSessionTokenResponse getSessionTokenResponse) {
        if (getSessionTokenResponse == null) {
            if (debugging()) {
                System.out.println(getClass() + " - createNewSessionToken, return null - GetSessionTokenResponse null");
            }
            return null;
        }

        if (getSessionTokenResponse.hasError()) {
            if (debugging()) {
                System.out.println(getClass() + " - createNewSessionToken, return null - GetSessionTokenResponse has error");
            }
            return null;
        }

        String tokenString = getSessionTokenResponse.getSessionToken();
        String secretKey = getSessionTokenResponse.getSecretKey();
        String time = getSessionTokenResponse.getTime();
        String pkey = getSessionTokenResponse.getPkey();
        String ekey = getSessionTokenResponse.getEkey();

        SessionToken.Builder builder = new SessionToken.Builder(tokenString);
        builder.secretKey(secretKey).time(time).pkey(pkey).ekey(ekey);

        if (debugging()) {
            System.out.println(getClass() + " - createNewSessionToken, return new session token");
        }
        return builder.build();
    }
}
