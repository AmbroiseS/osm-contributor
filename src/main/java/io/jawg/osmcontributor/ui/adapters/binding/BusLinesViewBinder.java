package io.jawg.osmcontributor.ui.adapters.binding;

import android.app.Activity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import io.jawg.osmcontributor.OsmTemplateApplication;
import io.jawg.osmcontributor.R;
import io.jawg.osmcontributor.ui.adapters.BusLineAdapter;
import io.jawg.osmcontributor.ui.adapters.item.TagItem;
import io.jawg.osmcontributor.ui.adapters.parser.BusLineValueParserImpl;
import io.jawg.osmcontributor.ui.adapters.parser.ParserManager;
import io.jawg.osmcontributor.ui.utils.views.DividerItemDecoration;
import io.jawg.osmcontributor.ui.utils.views.holders.TagItemBusLineViewHolder;

public class BusLinesViewBinder extends CheckedTagViewBinder<TagItemBusLineViewHolder, TagItem> {

    @Inject
    BusLineValueParserImpl busLineValueParser;

    public BusLinesViewBinder(Activity activity, OnTagItemChange onTagItemChange) {
        super(activity, onTagItemChange);
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
        List<String> busLines = null;
        try {
            busLines = busLineValueParser.fromValue(tagItem.getValue());
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            tagItem.setConform(false);
        }

        if (busLines == null) {
            busLines = new ArrayList<>();
        }

        List<String> finalBusLines = busLines;


        final BusLineAdapter adapter = new BusLineAdapter(finalBusLines);
        adapter.setListener(() -> refreshData(tagItem, adapter, finalBusLines));

        holder.getBusLineRecyclerView().setAdapter(adapter);
        holder.getBusLineRecyclerView().setLayoutManager(new LinearLayoutManager(activity.get()));
        holder.getBusLineRecyclerView().setHasFixedSize(false);
        RecyclerView.ItemAnimator itemAnimator = new DefaultItemAnimator();
        itemAnimator.setAddDuration(300);
        holder.getBusLineRecyclerView().setItemAnimator(itemAnimator);
        holder.getBusLineRecyclerView().addItemDecoration(new DividerItemDecoration(activity.get()));

        holder.getEditAddButton().setOnClickListener(
                view -> {
                    if (!holder.getTextViewValue().getText().toString().trim().equals("")) {
                        finalBusLines.add(
                                busLineValueParser.cleanValue(
                                        holder.getTextViewValue().getText().toString()));
                        holder.getTextViewValue().getText().clear();
                        refreshData(tagItem, adapter, finalBusLines);
                    }
                });

    }

    private void refreshData(TagItem tagItem, BusLineAdapter adapter, List<String> finalBusLines) {
        tagItem.setValue(busLineValueParser.toValue(finalBusLines));
        adapter.notifyDataSetChanged();
        onTagItemChange.onTagItemUpdated(tagItem);
    }

    @Override
    public TagItemBusLineViewHolder onCreateViewHolder(ViewGroup parent) {
        View poiTagOpeningHoursLayout = LayoutInflater.from(parent.getContext()).inflate(R.layout.tag_item_bus_line, parent, false);
        return new TagItemBusLineViewHolder(poiTagOpeningHoursLayout);
    }

}
