package com.termux.app.quick;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.view.View;
import android.widget.RemoteViews;

import com.termux.R;
import com.termux.app.TermuxActivity;

import java.util.List;

public class TermnxWidgetProvider extends AppWidgetProvider {

    public static final String EXTRA_RUN_COMMAND = "com.termux.app.TERMNX_RUN_COMMAND";

    private static final int[] BUTTON_IDS = {
        R.id.termnx_qc_0, R.id.termnx_qc_1, R.id.termnx_qc_2,
        R.id.termnx_qc_3, R.id.termnx_qc_4, R.id.termnx_qc_5
    };

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId);
        }
    }

    private void updateWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.termnx_widget);
        List<String[]> commands = new TermnxQuickPrefs(context).getCommands();

        boolean any = false;
        for (int i = 0; i < BUTTON_IDS.length; i++) {
            if (i < commands.size()) {
                String[] entry = commands.get(i);
                views.setTextViewText(BUTTON_IDS[i], entry[0]);
                views.setViewVisibility(BUTTON_IDS[i], View.VISIBLE);

                Intent intent = new Intent(context, TermuxActivity.class);
                intent.setAction("com.termux.app.TERMNX_RUN_" + i + "_" + System.currentTimeMillis());
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                intent.putExtra(EXTRA_RUN_COMMAND, entry[1]);

                int flags = PendingIntent.FLAG_UPDATE_CURRENT;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    flags |= PendingIntent.FLAG_IMMUTABLE;
                }
                PendingIntent pending = PendingIntent.getActivity(context, i, intent, flags);
                views.setOnClickPendingIntent(BUTTON_IDS[i], pending);
                any = true;
            } else {
                views.setViewVisibility(BUTTON_IDS[i], View.GONE);
            }
        }

        views.setViewVisibility(R.id.termnx_widget_empty, any ? View.GONE : View.VISIBLE);
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    public static void refresh(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        ComponentName component = new ComponentName(context, TermnxWidgetProvider.class);
        int[] ids = manager.getAppWidgetIds(component);
        if (ids != null && ids.length > 0) {
            Intent intent = new Intent(context, TermnxWidgetProvider.class);
            intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
            context.sendBroadcast(intent);
        }
    }
}
