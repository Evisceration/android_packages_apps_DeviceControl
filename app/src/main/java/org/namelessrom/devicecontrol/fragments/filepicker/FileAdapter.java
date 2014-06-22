package org.namelessrom.devicecontrol.fragments.filepicker;

import android.content.res.Resources;
import android.text.format.Formatter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.namelessrom.devicecontrol.Application;
import org.namelessrom.devicecontrol.R;
import org.namelessrom.devicecontrol.utils.providers.BusProvider;

import java.io.File;
import java.util.Date;

import static butterknife.ButterKnife.findById;

/**
 * Created by alex on 22.06.14.
 */
public class FileAdapter extends BaseAdapter {

    public static final int TYPE_DIRECTORY = 1;
    public static final int TYPE_FILE      = 2;

    private File[] files;

    public FileAdapter(final File[] files) {
        this.files = files;
    }

    @Override public int getCount() { return files.length; }

    @Override public Object getItem(final int position) { return files[position]; }

    @Override public long getItemId(final int position) {
        if (files[position].isDirectory()) return TYPE_DIRECTORY;
        return TYPE_FILE;
    }

    private static final class ViewHolder {
        private final View      rootView;
        private final ImageView icon;
        private final TextView  name;
        private final TextView  info;

        private ViewHolder(final View rootView) {
            this.rootView = rootView;
            this.icon = findById(rootView, R.id.file_icon);
            this.name = findById(rootView, R.id.file_name);
            this.info = findById(rootView, R.id.file_info);
        }
    }

    @Override public View getView(final int position, View v, final ViewGroup parent) {
        final ViewHolder viewHolder;
        if (v == null) {
            v = Application.getLayoutInflater().inflate(R.layout.list_item_file, parent, false);
            assert (v != null);
            viewHolder = new ViewHolder(v);
            v.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) v.getTag();
        }

        final File file = files[position];

        viewHolder.rootView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BusProvider.getBus().post(file);
            }
        });

        final Resources resources = Application.applicationContext.getResources();

        viewHolder.name.setText(file.getName());

        if (file.isDirectory()) {
            viewHolder.icon.setImageDrawable(resources.getDrawable(R.drawable.ic_directory));
            viewHolder.info.setText(String.valueOf(new Date(file.lastModified())));
        } else {
            viewHolder.icon.setImageDrawable(resources.getDrawable(R.drawable.ic_file));
            viewHolder.info.setText(String.valueOf(new Date(file.lastModified())) + " | "
                    + Formatter.formatFileSize(Application.applicationContext, file.length()));
        }

        return v;
    }
}
