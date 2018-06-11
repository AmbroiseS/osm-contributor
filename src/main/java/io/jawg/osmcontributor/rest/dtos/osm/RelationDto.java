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
package io.jawg.osmcontributor.rest.dtos.osm;

import org.joda.time.DateTime;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.ArrayList;
import java.util.List;

/**
 * the complete relation Dto
 */
@Root(name = "relation", strict = false)
public class RelationDto {

    @Attribute(required = false)
    private String id;

    @Attribute(required = false)
    private Long changeset;

    @Attribute(required = false)
    private DateTime timestamp;

    @Attribute(required = false)
    private int version;

    @Attribute(required = false)
    private String user;

    @Attribute(required = false)
    private String uid;

    @Attribute(required = false)
    private CenterDto centerDto;

    @ElementList(inline = true, required = false)
    private List<TagDto> tagsDtoList = new ArrayList<>();

    @ElementList(inline = true, required = false)
    private List<RelationMemberDto> memberDTOlist = new ArrayList<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Long getChangeset() {
        return changeset;
    }

    public void setChangeset(Long changeset) {
        this.changeset = changeset;
    }

    public DateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(DateTime timestamp) {
        this.timestamp = timestamp;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public List<TagDto> getTagsDtoList() {
        return tagsDtoList;
    }

    public void setTagsDtoList(List<TagDto> tagsDtoList) {
        this.tagsDtoList = tagsDtoList;
    }

    public List<RelationMemberDto> getMemberDTOlist() {
        return memberDTOlist;
    }

    public CenterDto getCenterDto() {
        return centerDto;
    }

    public void setCenterDto(CenterDto centerDto) {
        this.centerDto = centerDto;
    }

    public void setMemberDTOlist(List<RelationMemberDto> memberDTOlist) {

        this.memberDTOlist = memberDTOlist;
    }
}
