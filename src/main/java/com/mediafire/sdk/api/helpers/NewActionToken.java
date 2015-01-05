package com.mediafire.sdk.api.helpers;

import com.mediafire.sdk.api.responses.user.GetActionTokenResponse;
import com.mediafire.sdk.config.ITokenManager;
import com.mediafire.sdk.http.Request;
import com.mediafire.sdk.http.Response;
import com.mediafire.sdk.token.ActionToken;
import com.mediafire.sdk.token.ImageActionToken;
import com.mediafire.sdk.token.UploadActionToken;

/**
 * Created by Chris on 11/9/2014.
 * BaseClientHelper used for fetching new action tokens, the only difference between NewActionTokenClientHelper
 * and ApiClientHelper is that returnToken() also needs to return an action token to the ActionTokenManagerInterface
 */
public class NewActionToken extends UseSessionToken {

    private String mTokenType;
    private ITokenManager mActionITokenManagerInterface;

    public NewActionToken(String tokenType, ITokenManager actionITokenManagerInterface) {
        super(actionITokenManagerInterface);

        mTokenType = tokenType;
        mActionITokenManagerInterface = actionITokenManagerInterface;
    }

    @Override
    public void borrowToken(Request request) {
        super.borrowToken(request);
    }

    @Override
    public void addSignatureToRequestParameters(Request request) {
        super.addSignatureToRequestParameters(request);
    }

    @Override
    public void returnToken(Response response, Request request) {
        super.returnToken(response, request);
        // in addition to calling super.returnToken(), the action token needs to be passed to
        // the ActionTokenManagerInterface

        GetActionTokenResponse getActionTokenResponse = getResponseObject(response, GetActionTokenResponse.class);

        if (getActionTokenResponse == null) {
            mActionITokenManagerInterface.tokensBad();
            return;
        }

        boolean badRequest = false;
        if (getActionTokenResponse.hasError()) {
            if (debugging()) {
                System.out.println(getClass() + " - returnToken, not returning token, GetActionTokenResponse has error");
            }
            badRequest = true;
        }

        if (getActionTokenResponse.getError() == 105) {
            if (debugging()) {
                System.out.println(getClass() + " - returnToken, not returning token, GetActionTokenResponse has error 105");
            }
            badRequest = true;
        }

        if (getActionTokenResponse.getError() == 127) {

            if (debugging()) {
                System.out.println(getClass() + " - returnToken, not returning token, GetActionTokenResponse has error 127");
            }
            badRequest = true;
        }

        if (badRequest) {

            if (debugging()) {
                System.out.println(getClass() + " - returnToken, not returning token, notifying ActionTokenManager tokens bad");
            }
            mActionITokenManagerInterface.tokensBad();
            return;
        }

        if ("image".equals(mTokenType)) {
            ImageActionToken mfImageActionToken = (ImageActionToken) createActionToken(ImageActionToken.class, getActionTokenResponse, request);
            mActionITokenManagerInterface.give(mfImageActionToken);
        } else if ("upload".equals(mTokenType)) {
            UploadActionToken uploadActionToken = (UploadActionToken) createActionToken(UploadActionToken.class, getActionTokenResponse, request);
            mActionITokenManagerInterface.give(uploadActionToken);
        } else {
            if (debugging()) {
                System.out.println(getClass() + " - returnToken, not returning token, type " + mTokenType + " not valid");
            }
        }
    }

    private ActionToken createActionToken(Class<? extends ActionToken> clazz, GetActionTokenResponse getActionTokenResponse, Request request) {
        if (getActionTokenResponse == null) {
            if (debugging()) {
                System.out.println(getClass() + " - createActionToken, returning null, GetActionTokenResponse null");
            }
            return null;
        }

        if (getActionTokenResponse.hasError()) {
            if (debugging()) {
                System.out.println(getClass() + " - createActionToken, returning null, GetActionTokenResponse has error");
            }
            return null;
        }

        String tokenString = getActionTokenResponse.getActionToken();
        long tokenExpiry;
        if (request.getQueryParameters().containsKey("lifespan")) {
            Object lifeSpanParam = request.getQueryParameters().get("lifespan");
            String lifeSpanParamAsString = String.valueOf(lifeSpanParam);
            tokenExpiry = Long.valueOf(lifeSpanParamAsString);
        } else {
            tokenExpiry = 0;
        }

        // lifespan is in minutes, but milliseconds needed by ImageActionToken
        int multiplier = 60 * 1000;

        tokenExpiry *= multiplier;

        if (clazz == ImageActionToken.class) {
            return new ImageActionToken(tokenString, tokenExpiry);
        } else if (clazz == UploadActionToken.class) {
            return new UploadActionToken(tokenString, tokenExpiry);
        } else {
            if (debugging()) {
                System.out.println(getClass() + " - createActionToken, returning null, class invalid: " + clazz);
            }

            return null;
        }
    }
}
