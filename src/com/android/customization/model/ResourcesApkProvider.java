package com.android.customization.model;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.ColorInt;

public class ResourcesApkProvider {
    private static final String TAG = "ResourcesApkProvider";

    protected final Context mContext;
    protected final String mStubPackageName;
    protected final Resources mStubApkResources;

    public ResourcesApkProvider(Context context, String stubPackageName) {
        mContext = context;
        mStubPackageName = stubPackageName;
        if (TextUtils.isEmpty(mStubPackageName)) {
            mStubApkResources = null;
        } else {
            Resources apkResources = null;
            try {
                PackageManager pm = mContext.getPackageManager();
                ApplicationInfo stubAppInfo = pm.getApplicationInfo(mStubPackageName,
                        PackageManager.GET_META_DATA | PackageManager.MATCH_SYSTEM_ONLY);
                if (stubAppInfo != null) {
                    apkResources = pm.getResourcesForApplication(stubAppInfo);
                }
            } catch (NameNotFoundException e) {
                Log.w(TAG, String.format("Stub APK for %s not found.", mStubPackageName));
            } finally {
                mStubApkResources = apkResources;
            }
        }
    }

    /**
     * Gets a string array resource from the stub aok.
     */
    public String[] getItemsFromStub(String arrayName) {
        int themesListResId = mStubApkResources.getIdentifier(arrayName, "array",  mStubPackageName);
        return mStubApkResources.getStringArray(themesListResId);
    }

    /**
     * Gets a string resource from the stub aok.
     */
    public String getItemStringFromStub(String prefix, String itemName) {
        int resourceId = mStubApkResources.getIdentifier(String.format("%s%s", prefix, itemName),
                "string", mStubPackageName);
        return mStubApkResources.getString(resourceId);
    }

    /**
     * Gets a drawable resource from the stub aok.
     */
    public Drawable getItemDrawableFromStub(String prefix, String itemName) {
        int resourceId = mStubApkResources.getIdentifier(String.format("%s%s", prefix, itemName),
                "drawable", mStubPackageName);
        return mStubApkResources.getDrawable(resourceId, null);
    }

    /**
     * Gets a color resource from the stub aok.
     */
    @ColorInt
    public int getItemColorFromStub(String prefix, String itemName) {
        int resourceId = mStubApkResources.getIdentifier(String.format("%s%s", prefix, itemName),
                "color", mStubPackageName);
        return mStubApkResources.getColor(resourceId, null);
    }

    public boolean isAvailable() {
        return mStubApkResources != null;
    }
}
