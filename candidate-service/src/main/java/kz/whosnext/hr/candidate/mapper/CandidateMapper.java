package kz.whosnext.hr.candidate.mapper;

import kz.whosnext.hr.candidate.model.dto.response.CandidateResponse;
import kz.whosnext.hr.candidate.model.entity.Candidate;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface CandidateMapper {
    CandidateResponse toResponse(Candidate candidate);
}

