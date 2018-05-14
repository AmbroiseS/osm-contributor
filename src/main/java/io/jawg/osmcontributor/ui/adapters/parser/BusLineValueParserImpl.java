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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class BusLineValueParserImpl implements ValueParser<List<String>> {
    public static final String SEP = ";";


    @Inject
    public BusLineValueParserImpl() {
    }


    @Override
    public String toValue(List<String> busLines) {
        StringBuilder builder = new StringBuilder();

        for (String s : busLines) {

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
            return null;
        }
        //todo more checks
        String[] openingTimes = data.split(SEP);
        return new ArrayList<>(Arrays.asList(openingTimes));
    }

    @Override
    public int getPriority() {
        return ParserManager.PRIORITY_HIGH;
    }

}
