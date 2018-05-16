package io.jawg.osmcontributor.ui.adapters.binding;

import android.app.Activity;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.lang.ref.WeakReference;

import io.jawg.osmcontributor.R;
import io.jawg.osmcontributor.ui.adapters.item.shelter.TagItem;

public abstract class CheckedTagViewBinder<T extends RecyclerView.ViewHolder, H extends TagItem> implements TagViewBinder<T, H> {

    public WeakReference<Activity> activity;
    public LinearLayout content;
    public TagItemChangeListener tagItemChangeListener;

    public CheckedTagViewBinder(Activity activity, TagItemChangeListener tagItemChangeListener) {
        this.activity = new WeakReference<>(activity);
        this.tagItemChangeListener = tagItemChangeListener;
    }

    /**
     * Check if the tag contains correct values
     *
     * @param tagItem tag currently checked
     */
    protected void showInvalidityMessage(TagItem tagItem) {
        // If the tag is not conform we show the error message, if not we remove it exists
        if (!tagItem.isConform() && content.getChildAt(1).getId() != R.id.malformated_layout) {
            content.addView(LayoutInflater.from(activity.get()).inflate(
                    R.layout.malformated_layout, content, false), 1);
            String currentValue = activity.get().getString(R.string.malformated_value) + " " + tagItem.getValue();
            ((TextView) ((LinearLayout) content.getChildAt(1)).getChildAt(1)).setText(currentValue);
        } else if (content.getChildAt(1).getId() == R.id.malformated_layout) {
            content.removeViewAt(1);
        }
    }

    public interface TagItemChangeListener {
        void onTagItemUpdated(TagItem updatedTag);
    }
}