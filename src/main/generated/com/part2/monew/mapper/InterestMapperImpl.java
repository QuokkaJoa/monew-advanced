package com.part2.monew.mapper;

import com.part2.monew.dto.request.InterestRegisterRequestDto;
import com.part2.monew.dto.response.InterestDto;
import com.part2.monew.entity.Interest;
import java.util.List;
import java.util.UUID;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-06-05T02:33:00+0900",
    comments = "version: 1.6.3, compiler: IncrementalProcessingEnvironment from gradle-language-java-8.14.jar, environment: Java 17.0.15 (Eclipse Adoptium)"
)
@Component
public class InterestMapperImpl implements InterestMapper {

    @Override
    public InterestDto toDto(Interest interest, boolean subscribedByMe) {
        if ( interest == null ) {
            return null;
        }

        UUID id = null;
        String name = null;
        Long subscriberCount = null;
        List<String> keywords = null;
        if ( interest != null ) {
            id = interest.getId();
            name = interest.getName();
            if ( interest.getSubscriberCount() != null ) {
                subscriberCount = interest.getSubscriberCount().longValue();
            }
            keywords = interestKeywordsToKeywordNames( interest.getInterestKeywords() );
        }
        boolean subscribedByMe1 = false;
        subscribedByMe1 = subscribedByMe;

        InterestDto interestDto = new InterestDto( id, name, keywords, subscriberCount, subscribedByMe1 );

        return interestDto;
    }

    @Override
    public Interest fromRegisterRequestDto(InterestRegisterRequestDto dto) {
        if ( dto == null ) {
            return null;
        }

        Interest interest = new Interest();

        interest.setName( dto.name() );

        return interest;
    }
}
