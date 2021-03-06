package sakethkaparthi.fileio.adapters;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import org.apache.commons.io.FilenameUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import sakethkaparthi.fileio.R;
import sakethkaparthi.fileio.activities.FileDescriptionActivity;

/**
 * Created by saketh on 19/10/16.
 */

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.ViewHolder> {
    private Cursor cursor;
    private Activity activity;

    public FileAdapter(Activity activity, Cursor cursor) {
        this.cursor = cursor;
        this.activity = activity;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        cursor.moveToPosition(position);
        holder.fileNameTextView.setText(cursor.getString(1));
        holder.iconImageView.setImageResource(getIconFromExtension(cursor.getString(1)));
        holder.shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cursor.moveToPosition(position);
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, "Download " + cursor.getString(1) + " from " + cursor.getString(2));
                sendIntent.setType("text/plain");
                activity.startActivity(sendIntent);
            }
        });
        int uploadedDay = getDateFromEpoch(cursor.getLong(3));
        int today = getDateFromEpoch(System.currentTimeMillis());
        int remaining = uploadedDay + 13 - today;
        if (remaining < 0) {
            holder.fileExpiryTextView.setText(R.string.file_expired);
            holder.fileExpiryTextView.setTextColor(ContextCompat.getColor(activity, R.color.colorAccent));
        } else {
            holder.fileExpiryTextView.setText(String.format(activity.getResources().getString(R.string.expires_in), remaining));
        }
        holder.cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(activity, FileDescriptionActivity.class).putExtra("item", position);
                ActivityOptionsCompat options = ActivityOptionsCompat.
                        makeSceneTransitionAnimation(activity, holder.iconImageView, "image");
                activity.startActivity(intent, options.toBundle());
            }
        });
    }

    @Override
    public int getItemCount() {
        return cursor.getCount();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView fileNameTextView, fileExpiryTextView;
        ImageView iconImageView;
        ImageButton shareButton;
        CardView cardView;

        ViewHolder(View v) {
            super(v);
            fileNameTextView = (TextView) v.findViewById(R.id.file_name_text_view);
            fileExpiryTextView = (TextView) v.findViewById(R.id.file_expiry_text_view);
            iconImageView = (ImageView) v.findViewById(R.id.file_icon);
            shareButton = (ImageButton) v.findViewById(R.id.share_button);
            cardView = (CardView) v.findViewById(R.id.card_view);
        }
    }

    int getDateFromEpoch(long milliseconds) {
        Date date = new Date(milliseconds);
        DateFormat formatter = new SimpleDateFormat("DDD", Locale.UK);
        String dateFormatted = formatter.format(date);
        return Integer.valueOf(dateFormatted);
    }

    private int getIconFromExtension(String filename) {
        String extension = FilenameUtils.getExtension(filename);
        if (extension.equalsIgnoreCase("docx") || extension.equalsIgnoreCase("doc"))
            return R.drawable.ic_doc;
        if (extension.equalsIgnoreCase("jpg") || extension.equalsIgnoreCase("jpeg"))
            return R.drawable.ic_jpg;
        if (extension.equalsIgnoreCase("avi"))
            return R.drawable.ic_avi;
        if (extension.equalsIgnoreCase("mp3"))
            return R.drawable.ic_mp3;
        if (extension.equalsIgnoreCase("png"))
            return R.drawable.ic_png;
        if (extension.equalsIgnoreCase("zip"))
            return R.drawable.ic_zip;
        if (extension.equalsIgnoreCase("mp4"))
            return R.drawable.ic_mp4;
        if (extension.equalsIgnoreCase("pdf"))
            return R.drawable.ic_pdf;
        if (extension.equalsIgnoreCase("ppt") || extension.equalsIgnoreCase("pptx"))
            return R.drawable.ic_ppt;
        if (extension.equalsIgnoreCase("exe"))
            return R.drawable.ic_exe;
        if (extension.equalsIgnoreCase("iso"))
            return R.drawable.ic_iso;
        if (extension.equalsIgnoreCase("txt"))
            return R.drawable.ic_txt;
        if (extension.equalsIgnoreCase("xml"))
            return R.drawable.ic_xml;
        if (extension.equalsIgnoreCase("xls"))
            return R.drawable.ic_xls;

        return R.drawable.ic_file;
    }
}
