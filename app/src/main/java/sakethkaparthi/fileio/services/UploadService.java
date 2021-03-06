package sakethkaparthi.fileio.services;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.google.gson.JsonObject;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import sakethkaparthi.fileio.R;
import sakethkaparthi.fileio.activities.MainActivity;
import sakethkaparthi.fileio.database.FilesContract;
import sakethkaparthi.fileio.models.ProgressRequestBody;
import sakethkaparthi.fileio.networkclients.RetrofitClient;
import sakethkaparthi.fileio.receivers.RetryReceiver;
import sakethkaparthi.fileio.widget.FilesWidgetProvider;

/**
 * Created by saketh on 19/10/16.
 */

public class UploadService extends IntentService {
    private NotificationManager mNotifyManager;
    private NotificationCompat.Builder mBuilder;

    public UploadService() {
        super("FileUploadService");
    }

    private static final String TAG = UploadService.class.getSimpleName();

    @Override
    protected void onHandleIntent(Intent intent) {
        Uri uri = Uri.parse(intent.getExtras().getString("uri"));
        final String displayName = intent.getExtras().getString("displayName");
        Log.d(TAG, "onHandleIntent: " + uri.toString() + " " + displayName);
        try {
            final InputStream inputStream = getContentResolver().openInputStream(uri);
            final OutputStream outputStream = openFileOutput(displayName, Context.MODE_PRIVATE);
            assert inputStream != null;
            IOUtils.copy(inputStream, outputStream);
            final File file = new File(getFilesDir(), displayName);
            final int id = (int) Math.floor(Math.random());
            Intent notificationIntent = new Intent(getApplicationContext(), RetryReceiver.class);
            notificationIntent.putExtra("uri", uri.toString());
            notificationIntent.putExtra("displayName", displayName);
            Log.d(TAG, "create intent: " + uri.toString() + " " + displayName);
            final PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), id, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            mNotifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            mBuilder = new NotificationCompat.Builder(this);
            mBuilder.setContentTitle(displayName)
                    .setContentText(getString(R.string.upload_in_progress))
                    .setSmallIcon(R.drawable.ic_cloud_upload);
            mBuilder.setProgress(100, 0, false);
            // Displays the progress bar for the first time.
            Notification notification = mBuilder.build();
            notification.flags |= Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;
            mNotifyManager.notify(id, notification);
            ProgressRequestBody fileBody = new ProgressRequestBody(file, new ProgressRequestBody.UploadCallbacks() {
                @Override
                public void onProgressUpdate(int percentage) {
                    Log.d(TAG, "onProgressUpdate: " + percentage);
                    mBuilder.setProgress(100, percentage, false);
                    // Displays the progress bar for the first time.
                    Notification notification = mBuilder.build();
                    notification.flags |= Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;
                    mNotifyManager.notify(id, notification);
                }

                @Override
                public void onError() {
                    mBuilder.setContentText(getString(R.string.error_uploading))
                            .setSmallIcon(android.R.drawable.stat_notify_error)
                            // Removes the progress bar
                            .setProgress(0, 0, false)
                            .setContentIntent(pendingIntent);

                    mNotifyManager.notify(id, mBuilder.build());
                }

                @Override
                public void onFinish() {
                    Log.d(TAG, "File upload finished");
                }
            });
            MultipartBody.Part filePart = MultipartBody.Part.createFormData("file", file.getName(), fileBody);
            Call<JsonObject> request = RetrofitClient.getAPI().uploadImage(filePart);
            request.enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                    try {
                        response.body().toString();
                        if (response.isSuccessful()) {
                            JSONObject object = new JSONObject(String.valueOf(response.body()));
                            String link = object.getString("link");
                            Log.d(TAG, "Success: " + link);
                            ContentValues values = new ContentValues();
                            values.put(FilesContract.FileEntry.COLUMN_NAME, displayName);
                            values.put(FilesContract.FileEntry.COLUMN_LINK, link);
                            values.put(FilesContract.FileEntry.COLUMN_UPLOAD_DATE, System.currentTimeMillis());
                            values.put(FilesContract.FileEntry.COLUMN_STATUS, 1);
                            Intent resultIntent = new Intent(getApplicationContext(), MainActivity.class);
                            PendingIntent resultPendingIntent =
                                    PendingIntent.getActivity(
                                            getApplicationContext(),
                                            0,
                                            resultIntent,
                                            PendingIntent.FLAG_UPDATE_CURRENT
                                    );
                            Log.d(TAG, "onResponse: Before notify");
                            mBuilder.setContentText(getString(R.string.upload_finished))
                                    // Removes the progress bar
                                    .setProgress(0, 0, false)
                                    .setContentIntent(resultPendingIntent)
                                    .setAutoCancel(true);
                            mNotifyManager.notify(id, mBuilder.build());
                            Log.d(TAG, "onResponse: After notify");
                            getContentResolver().insert(FilesContract.FileEntry.CONTENT_URI, values);
                            Intent intent = new Intent(getApplicationContext(), FilesWidgetProvider.class);
                            intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                            int widgetIDs[] = AppWidgetManager.getInstance(getApplication()).getAppWidgetIds(new ComponentName(getApplication(), FilesWidgetProvider.class));
                            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIDs);
                            sendBroadcast(intent);
                            boolean deleted = file.delete();
                            inputStream.close();
                            outputStream.close();
                            Log.d(TAG, "Deleted: " + deleted);
                        } else {
                            throw new Exception("Error while uploading");
                        }
                    } catch (Exception e) {
                        Log.d(TAG, "onResponse: " + e);
                        mBuilder.setContentText(getString(R.string.error_uploading))
                                .setSmallIcon(android.R.drawable.stat_notify_error)
                                // Removes the progress bar
                                .setProgress(0, 0, false)
                                .setContentIntent(pendingIntent);

                        mNotifyManager.notify(id, mBuilder.build());
                    }
                }

                @Override
                public void onFailure(Call<JsonObject> call, Throwable t) {
                    Log.d(TAG, "onFailure: " + t.getMessage());
                    mBuilder.setContentText(getString(R.string.error_uploading))
                            .setSmallIcon(android.R.drawable.stat_notify_error)
                            // Removes the progress bar
                            .setProgress(0, 0, false)
                            .setContentIntent(pendingIntent);

                    mNotifyManager.notify(id, mBuilder.build());
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mNotifyManager.cancelAll();
    }
}
