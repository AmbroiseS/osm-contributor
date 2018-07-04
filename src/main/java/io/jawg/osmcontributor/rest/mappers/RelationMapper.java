package io.jawg.osmcontributor.rest.mappers;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import io.jawg.osmcontributor.model.entities.relation.Relation;
import io.jawg.osmcontributor.model.entities.relation.RelationMember;
import io.jawg.osmcontributor.model.entities.relation.RelationTag;
import io.jawg.osmcontributor.rest.dtos.osm.RelationDto;
import io.jawg.osmcontributor.rest.dtos.osm.RelationMemberDto;
import io.jawg.osmcontributor.rest.dtos.osm.TagDto;

public class RelationMapper {
    @Inject
    public RelationMapper() {

    }

    public List<Relation> convertDTOstoRelations(List<RelationDto> dtos) {
        List<Relation> result = new ArrayList<>();
        if (dtos != null) {
            for (RelationDto dto : dtos) {
                Relation relation = convertDTOtoRelation(dto);
                if (relation != null) {
                    result.add(relation);
                }
            }
        }
        return result;
    }

    private Relation convertDTOtoRelation(RelationDto dto) {
        List<RelationMember> relationMembers = new ArrayList<>();
        List<RelationTag> tags = new ArrayList<>();


        Relation relation = new Relation();
        relation.setBackendId(dto.getId());
        relation.setChangeset(dto.getChangeset());
        relation.setUpdated(false);
        relation.setVersion(String.valueOf(dto.getVersion()));
        relation.setUpdateDate(dto.getTimestamp());

        for (RelationMemberDto re : dto.getMemberDTOlist()) {
            RelationMember relationMember = new RelationMember();
            relationMember.setRelation(relation);
            if (re.getRef() != null)
                relationMember.setRef(re.getRef());
            if (re.getRole() != null)
                relationMember.setRole(re.getRole());
            if (re.getType() != null)
                relationMember.setType(re.getType());
            relationMembers.add(relationMember);
        }
        relation.setMembers(relationMembers);

        for (TagDto tagDto : dto.getTagsDtoList()) {
            RelationTag tag = new RelationTag();
            tag.setRelationDisplay(relation);
            if (tagDto.getKey() != null && tagDto.getValue() != null) {
                tag.setKey(tagDto.getKey());
                tag.setValue(tagDto.getValue());
            }
            tags.add(tag);
            if (tag.getKey().equals("name")) {
                relation.setName(tag.getValue());
            }
        }
        relation.setTags(tags);
        return relation;
    }

    public RelationDto convertRelationToDTO(Relation relation, String changeSetId) {
        RelationDto relationDto = new RelationDto();
        List<RelationMemberDto> relationMemberDtos = new ArrayList<>();
        List<TagDto> tagDtos = new ArrayList<>();

        if (relation.getMembers() != null) {
            for (RelationMember re : relation.getMembers()) {
                RelationMemberDto relationMemberDto = new RelationMemberDto();
                relationMemberDto.setRef(re.getRef());
                relationMemberDto.setRole(re.getRole());
                relationMemberDto.setType(re.getType());
                relationMemberDtos.add(relationMemberDto);
            }
        }

        if (relation.getTags() != null) {
            for (RelationTag ta : relation.getTags()) {
                TagDto tagDto = new TagDto();
                tagDto.setKey(ta.getKey());
                tagDto.setValue(ta.getValue());
                tagDtos.add(tagDto);
            }
        }
        relationDto.setMemberDTOlist(relationMemberDtos);
        relationDto.setTagsDtoList(tagDtos);
        relationDto.setId(relation.getBackendId());
        relationDto.setChangeset(changeSetId);

        return relationDto;
    }

}
