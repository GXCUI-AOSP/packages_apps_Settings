package com.android.settings.cyanogenmod;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.util.Log;

import com.android.settings.applications.ProtectedAppsActivity;

import java.util.ArrayList;
import java.util.HashSet;

public class ProtectedAppsReceiver extends BroadcastReceiver {
    private static final String TAG = "ProtectedAppsReceiver";

    public static final String PROTECTED_ACTION = "cyanogenmod.intent.action.PACKAGE_PROTECTED";
    private static final String PROTECTED_CHANGED_ACTION =
            "cyanogenmod.intent.action.PROTECTED_COMPONENT_UPDATE";
    public static final String PROTECTED_STATE =
            "cyanogenmod.intent.action.PACKAGE_PROTECTED_STATE";
    public static final String PROTECTED_COMPONENTS =
            "cyanogenmod.intent.action.PACKAGE_PROTECTED_COMPONENTS";
    private static final String PROTECTED_APP_PERMISSION = "cyanogenmod.permission.PROTECTED_APP";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (PROTECTED_ACTION.equals(intent.getAction())) {
            boolean protect = intent.getBooleanExtra(PROTECTED_STATE,
                    PackageManager.COMPONENT_VISIBLE_STATUS);
            ArrayList<ComponentName> components =
                    intent.getParcelableArrayListExtra(PROTECTED_COMPONENTS);
            updateProtectedAppComponentsAndNotify(context, components, protect);
        }
    }

    public static void updateProtectedAppComponentsAndNotify(Context context,
            ArrayList<ComponentName> components, boolean state) {
        updateProtectedAppComponents(context, components, state);
        updateSettingsSecure(context, components, state);
        notifyProtectedChanged(context, components, state);
    }

    public static void updateProtectedAppComponents(Context context,
            ArrayList<ComponentName> components, boolean state) {
        PackageManager pm = context.getPackageManager();
        for (ComponentName component : components) {
            try {
                pm.setComponentProtectedSetting(component, state);
            } catch (NoSuchMethodError nsm) {
                Log.e(TAG, "Unable to protected app via PackageManager");
            }
        }
    }

    public static void updateSettingsSecure(Context context,
            ArrayList<ComponentName> components, boolean state) {
        HashSet<ComponentName> newComponentList = ProtectedAppsActivity.getComponentList(
                context, Settings.Secure.PROTECTED_COMPONENTS);

        boolean update = state == PackageManager.COMPONENT_PROTECTED_STATUS
            ? newComponentList.addAll(components)
            : newComponentList.removeAll(components);

        if (update) {
            ProtectedAppsActivity.putComponentList(context,
                    Settings.Secure.PROTECTED_COMPONENTS, newComponentList);
        }
    }

    public static void notifyProtectedChanged(Context context,
            ArrayList<ComponentName> components, boolean state) {
        Intent intent = new Intent(PROTECTED_CHANGED_ACTION);
        intent.putExtra(PROTECTED_STATE, state);
        intent.putExtra(PROTECTED_COMPONENTS, components);

        context.sendBroadcast(intent, PROTECTED_APP_PERMISSION);
    }
}
