/**
 * Copyright (C) 2016 eBusiness Information
 * <p>
 * This file is part of OSM Contributor.
 * <p>
 * OSM Contributor is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * OSM Contributor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with OSM Contributor.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.jawg.osmcontributor.ui.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import io.jawg.osmcontributor.R;
import io.jawg.osmcontributor.ui.managers.PoiManager;
import timber.log.Timber;

public class BusLineSuggestionAdapter extends BaseAdapter implements Filterable {

    private static final int LAYOUT = R.layout.simple_dropdown_item;
    private LayoutInflater layoutInflater;
    private List<String> suggestionsList;
    private Filter filter;
    private PoiManager poiManager;

    public BusLineSuggestionAdapter(Context context, PoiManager poiManager) {
        layoutInflater = LayoutInflater.from(context);
        suggestionsList = new ArrayList<>();
        filter = new SuggestionFilter();
        this.poiManager =poiManager;
    }

    @Override
    public int getCount() {
        return suggestionsList.size();
    }

    @Override
    public String getItem(int position) {
        return suggestionsList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view;
        ViewHolder holder;

        if (convertView == null) {
            view = layoutInflater.inflate(LAYOUT, parent, false);
            holder = new ViewHolder(view);
            view.setTag(holder);
        } else {
            view = convertView;
            holder = (ViewHolder) convertView.getTag();
        }

        holder.bind(suggestionsList.get(position));

        return view;
    }

    @Override
    public Filter getFilter() {
        return filter;
    }

    private static class ViewHolder {

        private final TextView textView;

        public ViewHolder(View root) {
            textView = (TextView) root.findViewById(android.R.id.text1);
        }

        public void bind(String item) {
            textView.setText(item);
        }
    }

    private class SuggestionFilter extends Filter {

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            if (constraint == null) {
                Timber.d("Null constraint in performFiltering");
                return null;
            }
            String query = constraint.toString().trim();
            List values = poiManager.getValuesForBusLinesAutocompletion(query);

            FilterResults results = new FilterResults();
            results.values = values;
            results.count = values.size();
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            if (results == null) {
                Timber.d("Null results in publishResults");
                return;
            }

            //noinspection unchecked
            suggestionsList = (List<String>) results.values;
            if (results.count > 0) {
                notifyDataSetChanged();
            } else {
                notifyDataSetInvalidated();
            }
        }

        @Override
        public CharSequence convertResultToString(Object resultValue) {
            return (String) resultValue;
        }
    }
}
