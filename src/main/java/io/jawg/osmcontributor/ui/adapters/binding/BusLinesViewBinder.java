package io.jawg.osmcontributor.ui.adapters.binding;

import android.app.Activity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import io.jawg.osmcontributor.OsmTemplateApplication;
import io.jawg.osmcontributor.R;
import io.jawg.osmcontributor.ui.adapters.BusLineAdapter;
import io.jawg.osmcontributor.ui.adapters.BusLineSuggestionAdapter;
import io.jawg.osmcontributor.ui.adapters.item.shelter.TagItem;
import io.jawg.osmcontributor.ui.adapters.parser.BusLineValueParserImpl;
import io.jawg.osmcontributor.ui.adapters.parser.ParserManager;
import io.jawg.osmcontributor.ui.managers.PoiManager;
import io.jawg.osmcontributor.ui.utils.views.DividerItemDecoration;
import io.jawg.osmcontributor.ui.utils.views.holders.TagItemBusLineViewHolder;

public class BusLinesViewBinder extends CheckedTagViewBinder<TagItemBusLineViewHolder, TagItem> {

    @Inject
    BusLineValueParserImpl busLineValueParser;

    @Inject
    PoiManager poiManager;

    private AutoCompleteTextView modelText;

    public BusLinesViewBinder(Activity activity, TagItemChangeListener tagItemChangeListener) {
        super(activity, tagItemChangeListener);
        ((OsmTemplateApplication) activity.getApplication()).getOsmTemplateComponent().inject(this);
    }

    @Override
    public boolean supports(TagItem.Type type) {
        return TagItem.Type.BUS_LINE.equals(type);
    }

    @Override
    public void onBindViewHolder(TagItemBusLineViewHolder holder, TagItem tagItem) {
        // Save holder
        this.content = holder.getContent();

        holder.getTextViewKey().setText(ParserManager.parseTagName(tagItem.getKey(), holder.getContent().getContext()));
        final List<String> busLines = new ArrayList<>();
        try {
            busLines.addAll(busLineValueParser.fromValue(tagItem.getValue()));
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            tagItem.setConform(false);
        }

        BusLineAdapter adapter = new BusLineAdapter(busLines);

        RecyclerView recyclerView = holder.getBusLineRecyclerView();
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(activity.get()));
        recyclerView.setHasFixedSize(false);
        RecyclerView.ItemAnimator itemAnimator = new DefaultItemAnimator();
        itemAnimator.setAddDuration(300);
        recyclerView.setItemAnimator(itemAnimator);
        recyclerView.addItemDecoration(new DividerItemDecoration(activity.get()));

        adapter.setRemoveBusListener((lineRemoved) -> {
            tagItem.setValue(busLineValueParser.toValue(busLines));
            onTagChange(tagItem);
        });

        holder.getEditAddButton().setOnClickListener(
                view -> {
                    String lineValue = holder.getTextViewValue().getText().toString();
                    if (!lineValue.trim().equals("")) {
                        addBusLine(tagItem, busLines, adapter, lineValue);
                    }
                    holder.getTextViewValue().getText().clear();
                });

        BusLineSuggestionAdapter suggestionAdapter = new BusLineSuggestionAdapter(activity.get().getApplicationContext(), poiManager, busLineValueParser, busLines);
        modelText = holder.getTextViewValue();
        modelText.setAdapter(suggestionAdapter);
        modelText.setOnItemClickListener((parent, view, position, id) -> {
            addBusLine(tagItem, busLines, adapter, suggestionAdapter.getItem(position));
            holder.getTextViewValue().getText().clear();
        });
      //  modelText.setDropDownHeight((int) (120 * activity.get().getApplication().getResources().getDisplayMetrics().density));
    }

    private void addBusLine(TagItem tagItem, List<String> busLines, BusLineAdapter adapter, String lineValue) {
        if (busLineValueParser.lineContainsMultipleValues(lineValue)) {
            List<String> temp= busLineValueParser.fromValue(lineValue);
            busLines.addAll(temp);
            adapter.notifyItemRangeInserted(busLines.size()-temp.size(), busLines.size()-1);
        } else {
            busLines.add(busLineValueParser.cleanValue(lineValue));
            adapter.notifyItemInserted(busLines.size() - 1);
        }
        tagItem.setValue(busLineValueParser.toValue(busLines));
        onTagChange(tagItem);
    }

    private void onTagChange(TagItem tagItem) {
        if (tagItemChangeListener != null) {
            tagItemChangeListener.onTagItemUpdated(tagItem);
        }
    }

    @Override
    public TagItemBusLineViewHolder onCreateViewHolder(ViewGroup parent) {
        View poiTagOpeningHoursLayout = LayoutInflater.from(parent.getContext()).inflate(R.layout.tag_item_bus_line, parent, false);
        return new TagItemBusLineViewHolder(poiTagOpeningHoursLayout);
    }

}
