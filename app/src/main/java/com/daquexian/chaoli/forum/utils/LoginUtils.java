package com.daquexian.chaoli.forum.utils;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.daquexian.chaoli.forum.ChaoliApplication;
import com.daquexian.chaoli.forum.data.Me;
import com.daquexian.chaoli.forum.meta.Constants;
import com.daquexian.chaoli.forum.model.TokenResult;
import com.daquexian.chaoli.forum.network.MyOkHttp;
import com.daquexian.chaoli.forum.network.MyOkHttp.Callback;
import com.daquexian.chaoli.forum.network.MyRetrofit;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Call;
import okhttp3.Response;

public class LoginUtils {
    @SuppressWarnings("unused")
    private static final String TAG = "LoginUtils";
    private static final String LOGIN_SP_NAME = "username_and_password";
    private static final String IS_LOGGED_IN = "is_logged_in";
    private static final String SP_USERNAME_KEY = "username";
    private static final String SP_PASSWORD_KEY = "password";
    public static final int FAILED_AT_OPEN_LOGIN_PAGE = 0;
    public static final int FAILED_AT_GET_TOKEN_ON_LOGIN_PAGE = 1;
    public static final int FAILED_AT_LOGIN = 2;
    public static final int WRONG_USERNAME_OR_PASSWORD = 3;
    public static final int FAILED_AT_OPEN_HOMEPAGE = 4;
    public static final int COOKIE_EXPIRED = 5;
    public static final int EMPTY_UN_OR_PW = 6;
    public static final int ERROR_LOGIN_STATUS = 7; // TODO: 17-1-2 handle this error

    private static void setToken(String token) {
        LoginUtils.token = token;
    }

    public static String getToken() {
        return token;
    }

    public static int getUserId() {
        return userId;
    }

    private static void setUserId(int userId) {
        LoginUtils.userId = userId;
    }

    private static String username;
    private static String password;
    private static String token;
    private static int userId;

    private static SharedPreferences sharedPreferences;

    public static void begin_login(final String username, final String password, final LoginObserver loginObserver){
        sharedPreferences = ChaoliApplication.getAppContext().getSharedPreferences(LOGIN_SP_NAME, Context.MODE_PRIVATE);

        if( !sharedPreferences.getBoolean(IS_LOGGED_IN, false)){
            LoginUtils.username = username;
            LoginUtils.password = password;
            pre_login(loginObserver);
        }else{
            //如果已经登录，先注销
            logout(new LogoutObserver() {
                @Override
                public void onLogoutSuccess() {
                    LoginUtils.username = username;
                    LoginUtils.password = password;
                    pre_login(loginObserver);
                }

                @Override
                public void onLogoutFailure(int statusCode) {
                    loginObserver.onLoginFailure(ERROR_LOGIN_STATUS);
                }
            });
        }
    }

    public static void begin_login(LoginObserver loginObserver){

        sharedPreferences = ChaoliApplication.getAppContext().getSharedPreferences(LOGIN_SP_NAME, Context.MODE_PRIVATE);
        Boolean is_logged_in = sharedPreferences.getBoolean(IS_LOGGED_IN, false);

        //if(CookieUtils.getCookie(context).size() != 0){

        username = sharedPreferences.getString(SP_USERNAME_KEY, "");
        password = sharedPreferences.getString(SP_PASSWORD_KEY, "");

        if(is_logged_in){
            getNewToken(loginObserver);
            //username = password = COOKIE_UN_AND_PW;
            return;
        }

        if("".equals(username) || "".equals(password)){
            loginObserver.onLoginFailure(EMPTY_UN_OR_PW);
            return;
        }

        Log.d("login", username + ", " + password);

        begin_login(username, password, loginObserver);
    }

    private static void pre_login(final LoginObserver loginObserver){//获取登录页面的token
        MyRetrofit.getService().getToken()
                .enqueue(new retrofit2.Callback<TokenResult>() {
                    @Override
                    public void onResponse(retrofit2.Call<TokenResult> call, retrofit2.Response<TokenResult> response) {
                        setToken(response.body().getToken());
                        login(loginObserver);
                    }

                    @Override
                    public void onFailure(retrofit2.Call<TokenResult> call, Throwable t) {
                        loginObserver.onLoginFailure(FAILED_AT_GET_TOKEN_ON_LOGIN_PAGE);
                    }
                });
        /* new MyOkHttp.MyOkHttpClient()
                .get(Constants.LOGIN_URL)
                .enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        e.printStackTrace();
                        loginObserver.onLoginFailure(FAILED_AT_GET_TOKEN_ON_LOGIN_PAGE);
                    }

                    @Override
                    public void onResponse(Call call, Response response, String responseStr) throws IOException {
                        String tokenFormat = "\"token\":\"([\\dabcdef]+)";
                        Pattern pattern = Pattern.compile(tokenFormat);
                        Matcher matcher = pattern.matcher(responseStr);
                        if (matcher.find()) {
                            setToken(matcher.group(1));
                            login(loginObserver);
                        } else {
                            //Log.e("regex_error", "regex_error");
                            loginObserver.onLoginFailure(FAILED_AT_GET_TOKEN_ON_LOGIN_PAGE);
                        }
                    }
                });
                */
    }

    private static void login(final LoginObserver loginObserver){ //发送请求登录
        new MyOkHttp.MyOkHttpClient()
                .add("username", username)
                .add("password", password)
                .add("return", "/")
                .add("login", "登录")
                .add("token", getToken())
                .post(Constants.LOGIN_URL)
                .enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        e.printStackTrace();
                        setSPIsLoggedIn(false);
                        loginObserver.onLoginFailure(COOKIE_EXPIRED);
                    }

                    @Override
                    public void onResponse(Call call, Response response, String responseStr) throws IOException {
                        String tokenFormat = "\"userId\":(\\d+),\"token\":\"([\\dabcdef]+)";
                        Pattern pattern = Pattern.compile(tokenFormat);
                        Matcher matcher = pattern.matcher(responseStr);
                        if (matcher.find()) {
                            int userId = Integer.parseInt(matcher.group(1));
                            setUserId(userId);
                            Me.setUserId(userId);

                            setToken(matcher.group(2));

                            saveUsernameAndPassword(username, password);
                            //CookieUtils.setCookies(CookieUtils.getCookie(context));
                            setSPIsLoggedIn(true);
                            Me.setUsername(username);
                            loginObserver.onLoginSuccess(getUserId(), getToken());
                        } else {
                            setSPIsLoggedIn(false);
                            loginObserver.onLoginFailure(WRONG_USERNAME_OR_PASSWORD);
                            //begin_login(context, loginObserver);
                            //Log.e("regex_error", "regex_error");
                        }

                    }
                });
    }

    private static void getNewToken(final LoginObserver loginObserver){ //得到新的token
        new MyOkHttp.MyOkHttpClient().get(Constants.HOMEPAGE_URL)
                .enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        e.printStackTrace();
                        setSPIsLoggedIn(false);
                        begin_login(loginObserver);
                    }

                    @Override
                    public void onResponse(Call call, Response response, String responseStr) throws IOException {
                        String tokenFormat = "\"userId\":(\\d+),\"token\":\"([\\dabcdef]+)";
                        Pattern pattern = Pattern.compile(tokenFormat);
                        Matcher matcher = pattern.matcher(responseStr);
                        if (matcher.find()) {
                            int userId = Integer.parseInt(matcher.group(1));
                            setUserId(userId);
                            Me.setUserId(userId);

                            setToken(matcher.group(2));

                            saveUsernameAndPassword(username, password);
                            //CookieUtils.setCookies(CookieUtils.getCookie(context));
                            setSPIsLoggedIn(true);
                            Me.setUsername(username);
                            loginObserver.onLoginSuccess(getUserId(), getToken());
                        } else {
                            setSPIsLoggedIn(false);
                            //loginObserver.onLoginFailure(COOKIE_EXPIRED);
                            begin_login(loginObserver);
                            //Log.e("regex_error", "regex_error");
                        }
                    }
                });
    }

    public static void logout(final LogoutObserver logoutObserver){
        String logoutURL = Constants.LOGOUT_PRE_URL + getToken();
        clear(ChaoliApplication.getAppContext());
        Me.clear();
        new MyOkHttp.MyOkHttpClient()
                .get(logoutURL)
                .enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        logoutObserver.onLogoutFailure(0);
                    }

                    @Override
                    public void onResponse(Call call, Response response, String responseStr) throws IOException {
                        logoutObserver.onLogoutSuccess();
                    }
                });
        /*client.get(context, logoutURL, new AsyncHttpResponseHandler() { //与服务器通信的作用似乎只是告诉服务器我下线了而已
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                logoutObserver.onLogoutSuccess();
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                Log.d(TAG, String.valueOf(statusCode));
                logoutObserver.onLogoutFailure(statusCode);
            }
        });*/
    }

    public static void clear(Context context){
        MyOkHttp.clearCookie();
        sharedPreferences = context.getSharedPreferences(LOGIN_SP_NAME, Context.MODE_PRIVATE);
        if(sharedPreferences != null) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.remove(IS_LOGGED_IN);
            editor.remove(SP_USERNAME_KEY);
            editor.remove(SP_PASSWORD_KEY);
            editor.apply();
        }
    }

    private static void setSPIsLoggedIn(Boolean isLoggedIn){
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(IS_LOGGED_IN, isLoggedIn);
        editor.apply();
    }

    public static Boolean isLoggedIn(){
        return sharedPreferences.getBoolean(IS_LOGGED_IN, false);
    }

    public static void saveUsernameAndPassword(String username, String password) {
        sharedPreferences = ChaoliApplication.getAppContext().getSharedPreferences(LOGIN_SP_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(SP_USERNAME_KEY, username);
        // TODO: 16-3-11 1915 Encrypt saved password
        editor.putString(SP_PASSWORD_KEY, password);
        editor.apply();
    }

    public interface LoginObserver
    {
        void onLoginSuccess(int userId, String token);
        void onLoginFailure(int statusCode);
    }

    public interface LogoutObserver
    {
        void onLogoutSuccess();
        void onLogoutFailure(int statusCode);
    }
}
