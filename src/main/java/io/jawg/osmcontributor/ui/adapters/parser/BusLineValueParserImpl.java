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
package io.jawg.osmcontributor.ui.adapters.parser;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class BusLineValueParserImpl implements ValueParser<List<String>> {
    private final String SEP = ";";

    @Inject
    public BusLineValueParserImpl() {
        //empty
    }

    @Override
    public String toValue(List<String> busLines) {
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < busLines.size(); i++) {
            if (i > 0) {
                builder.append(SEP);
            }
            builder.append(busLines.get(i));
        }
        return builder.toString();
    }

    //replace character used as separator in string to avoid parsing issues
    public String cleanValue(String value) {
        return value.replaceAll(SEP, ",");
    }

    @Override
    public List<String> fromValue(String data) {
        if (data == null || data.trim().isEmpty()) {
            return Collections.emptyList();
        }
        String[] lines = data.split(SEP);
        List<String> toReturn = Arrays.asList(lines);
        for (String s : toReturn) {
            if (s == null || s.equals("")) {
                toReturn.remove(s);
            }

        }
        return toReturn;
    }

    @Override
    public int getPriority() {
        return ParserManager.PRIORITY_HIGH;
    }

}