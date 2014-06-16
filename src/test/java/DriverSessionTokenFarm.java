import com.arkhive.components.test_session_manager_fixes.module_credentials.ApplicationCredentials;
import com.arkhive.components.test_session_manager_fixes.module_credentials.CredentialsException;
import com.arkhive.components.test_session_manager_fixes.module_http_processor.HttpPeriProcessor;
import com.arkhive.components.test_session_manager_fixes.module_token_farm.TokenFarm;
import com.arkhive.components.test_session_manager_fixes.module_token_farm.TokenFarmException;

/**
 * Created by Chris Najar on 6/15/2014.
 */
public class DriverSessionTokenFarm {
    public static void main(String[] args) {
        String apiKey = "1ngvq4h5rn8om4at7u9884z9i3sbww44b923w5ee";
        String appId = "35";
        ApplicationCredentials applicationCredentials = null;
        try {
            applicationCredentials = new ApplicationCredentials(appId, apiKey);
        } catch (CredentialsException e) {
            e.printStackTrace();
        }
        HttpPeriProcessor httpPeriProcessor = new HttpPeriProcessor(5000, 5000);

        TokenFarm tokenFarm = TokenFarm.getInstance();
        if (tokenFarm == null) {
            try {
                tokenFarm = TokenFarm.newInstance(applicationCredentials, httpPeriProcessor);
            } catch (TokenFarmException e) {
                e.printStackTrace();
            }
        }
    }
}
