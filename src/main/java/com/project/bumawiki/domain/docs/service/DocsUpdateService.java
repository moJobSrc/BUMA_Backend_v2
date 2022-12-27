package com.project.bumawiki.domain.docs.service;

import com.project.bumawiki.domain.contribute.domain.Contribute;
import com.project.bumawiki.domain.docs.domain.Docs;
import com.project.bumawiki.domain.docs.domain.VersionDocs;
import com.project.bumawiki.domain.docs.domain.repository.DocsRepository;
import com.project.bumawiki.domain.docs.domain.repository.VersionDocsRepository;
import com.project.bumawiki.domain.docs.exception.NoUpdatablePostException;
import com.project.bumawiki.domain.docs.presentation.dto.DocsResponseDto;
import com.project.bumawiki.domain.docs.presentation.dto.DocsUpdateRequestDto;
import com.project.bumawiki.domain.user.entity.User;
import com.project.bumawiki.domain.user.exception.UserNotLoginException;
import com.project.bumawiki.domain.user.presentation.dto.UserResponseDto;
import com.project.bumawiki.global.annotation.ServiceWithTransactionalReadOnly;
import com.project.bumawiki.global.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;


@RequiredArgsConstructor
@ServiceWithTransactionalReadOnly
public class DocsUpdateService {
    private final DocsRepository docsRepository;
    private final VersionDocsRepository versionDocsRepository;

    @Transactional
    public DocsResponseDto execute(Long docsId, UserResponseDto userResponseDto,DocsUpdateRequestDto docsUpdateRequestDto){
        try {
            SecurityUtil.getCurrentUser().getUser().getAuthority();
        }catch(Exception e){
            throw UserNotLoginException.EXCEPTION;
        }

        VersionDocs savedVersionDocs = saveVersionDocs(docsUpdateRequestDto, docsId);
        Docs docs = setVersionDocsToDocs(savedVersionDocs);
        docs.setModifiedTime(savedVersionDocs.getThisVersionCreatedAt());

        setContribute(docs, userResponseDto);

        return new DocsResponseDto(docs);
    }


    @Transactional
    private void setContribute(Docs docs, UserResponseDto userResponseDto) {
        User user = SecurityUtil.getCurrentUser().getUser();
        Contribute contribute = Contribute.builder()
                .docs(docs)
                .contributor(user)
                .build();
        userResponseDto.updateContribute(contribute);
        docs.updateContribute(contribute);

        user.setContributeDocs(userResponseDto.getContributeDocs());
    }



    @Transactional
    private VersionDocs saveVersionDocs(DocsUpdateRequestDto docsUpdateRequestDto,Long docsId){
        return versionDocsRepository.save(
                VersionDocs.builder()
                        .docsId(docsId)
                        .thisVersionCreatedAt(LocalDateTime.now())
                        .contents(docsUpdateRequestDto.getContents())
                        .build()
        );
    }

    @Transactional
    private Docs setVersionDocsToDocs(VersionDocs versionDocs){
        Docs docs = docsRepository.findById(versionDocs.getDocsId())
                .orElseThrow(() -> NoUpdatablePostException.EXCEPTION);

        docs.getDocsVersion().add(versionDocs);

        return docs;
    }

    public UserResponseDto findCurrentUser(){
        User user = SecurityUtil.getCurrentUser().getUser();

        return new UserResponseDto(user);
    }

    /**
     * 프론트가 [사진1]이라고 보낸거 우리가 저장한 이미지 주소로 바꾸는 로직
     */
    public void setImageUrlInContents(){

    }
}
