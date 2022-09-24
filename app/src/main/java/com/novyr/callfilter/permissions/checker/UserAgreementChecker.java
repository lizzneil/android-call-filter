package com.novyr.callfilter.permissions.checker;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;

import androidx.appcompat.app.AlertDialog;

import com.novyr.callfilter.R;
import com.novyr.callfilter.permissions.PermissionChecker;


//用户同意 隐私协议 同意让用的权限才用。promise  。 这里不要加权限判断，简化逻辑，单一职责。也不管UI。
// 单一职责后，前后台 UI 逻辑类都能用。
public class UserAgreementChecker implements CheckerInterface {

    private static final String TAG = UserAgreementChecker.class.getSimpleName();

    private final String mFunctionName;

   final PermissionChecker callback;
    //前置条件。某个功能需什么手机权限，什么roleManager,需要什么SAF。。。。。
//    private final List<CheckerInterface> mPreConditions = new ArrayList<>();


    public UserAgreementChecker(final String aFunctionName,final PermissionChecker aCallback) {
        mFunctionName = aFunctionName;
        callback = aCallback;
    }


    @Override
    public boolean hasAccess(Activity activity) {
//        //前置条件（如权限）被关，视为用户不许可
//        for(CheckerInterface oneCheck: mPreConditions){
//            if(oneCheck.hasAccess(activity)){
//                continue;
//            } else {
//                return false;
//            }
//        }
//        //前置条件开了
        return hasRequested(activity,mFunctionName);
    }

    /**
     * 这里要弹统一的UI，请求权限。
     *
     */
    @Override
    public boolean requestAccess(Activity activity, boolean forceAttempt) {
        if(hasAccess(activity)){
            return false;
        }
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        StringBuffer sb = new StringBuffer();
        sb.append("申请用户许可： ").append(activity.getString(R.string.user_agreement_tips));
        builder.setMessage(sb.toString())
                .setPositiveButton(R.string.agree, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // START THE GAME!
                        setPromise(activity,mFunctionName,true);
                        callback.onRequestUserAgreeResult(true);

                    }
                })
                .setNegativeButton(R.string.reject, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancelled the dialog
                        setPromise(activity,mFunctionName,false);
                        callback.onRequestUserAgreeResult(false);
                    }
                });
        // Create the AlertDialog object and return it
        AlertDialog dialog =  builder.create();
        dialog.show();

        return true;
    }

    private SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences(
                context.getString(R.string.user_agreement_preferences_file),
                Context.MODE_PRIVATE
        );
    }

    private boolean hasRequested(Context context, String functionName) {
        return getSharedPreferences(context).getBoolean(
                String.format("agree-%s", functionName),
                false
        );
    }


    private void setPromise(Context context, String functionName,boolean agree) {
        getSharedPreferences(context).edit()
                .putBoolean(String.format("agree-%s", functionName), agree)
                .apply();
    }
}
