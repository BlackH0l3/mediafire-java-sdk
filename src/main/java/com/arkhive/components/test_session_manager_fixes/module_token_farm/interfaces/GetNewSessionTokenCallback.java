package com.arkhive.components.test_session_manager_fixes.module_token_farm.interfaces;

import com.arkhive.components.test_session_manager_fixes.module_api_descriptor.ApiRequestObject;

/**
 * Created by on 6/24/2014.
 */
public interface GetNewSessionTokenCallback {
    public void receiveNewSessionToken(ApiRequestObject apiRequestObject);
}