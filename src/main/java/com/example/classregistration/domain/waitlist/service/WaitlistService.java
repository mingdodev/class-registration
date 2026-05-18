package com.example.classregistration.domain.waitlist.service;

import com.example.classregistration.domain.klass.repository.KlassRepository;
import com.example.classregistration.domain.klass.model.Klass;
import com.example.classregistration.domain.klassmate.repository.KlassmateRepository;
import com.example.classregistration.domain.klassmate.model.Klassmate;
import com.example.classregistration.domain.waitlist.repository.WaitlistRepository;
import com.example.classregistration.domain.waitlist.dto.MyWaitlistStatusResponse;
import com.example.classregistration.domain.waitlist.model.Waitlist;
import com.example.classregistration.global.exception.BusinessException;
import com.example.classregistration.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WaitlistService {

    private final KlassRepository klassRepository;
    private final KlassmateRepository klassmateRepository;
    private final WaitlistRepository waitlistRepository;

    @Transactional
    public void joinWaitlist(Long klassmateId, Long klassId) {
        Klass klass = findKlassById(klassId);
        klass.validateWaitlistJoinable();

        if (waitlistRepository.existsByKlassmateIdAndKlassId(klassmateId, klassId)) {
            throw new BusinessException(ErrorCode.WAITLIST_ALREADY_EXISTS);
        }

        Klassmate klassmate = klassmateRepository.getReferenceById(klassmateId);
        waitlistRepository.save(Waitlist.create(klassmate, klass));
    }

    @Transactional
    public void leaveWaitlist(Long klassmateId, Long klassId) {
        findKlassById(klassId);
        Waitlist waitlist = waitlistRepository.findByKlassmateIdAndKlassId(klassmateId, klassId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WAITLIST_NOT_FOUND));
        waitlistRepository.delete(waitlist);
    }

    @Transactional(readOnly = true)
    public MyWaitlistStatusResponse getMyWaitlistStatus(Long klassmateId, Long klassId) {
        findKlassById(klassId);
        return waitlistRepository.findByKlassmateIdAndKlassId(klassmateId, klassId)
                .map(w -> new MyWaitlistStatusResponse(true, w.getCreatedAt()))
                .orElse(new MyWaitlistStatusResponse(false, null));
    }

    private Klass findKlassById(Long klassId) {
        return klassRepository.findById(klassId)
                .orElseThrow(() -> new BusinessException(ErrorCode.KLASS_NOT_FOUND));
    }
}
