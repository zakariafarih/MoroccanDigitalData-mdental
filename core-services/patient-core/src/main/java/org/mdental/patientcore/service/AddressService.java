package org.mdental.patientcore.service;

import lombok.RequiredArgsConstructor;
import org.mdental.commons.model.PageResponse;
import org.mdental.patientcore.exception.AddressNotFoundException;
import org.mdental.patientcore.exception.PatientNotFoundException;
import org.mdental.patientcore.mapper.AddressMapper;
import org.mdental.patientcore.model.dto.AddressRequest;
import org.mdental.patientcore.model.dto.AddressResponse;
import org.mdental.patientcore.model.entity.Address;
import org.mdental.patientcore.model.entity.AddressType;
import org.mdental.patientcore.repository.AddressRepository;
import org.mdental.patientcore.repository.PatientRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AddressService {

    private final AddressRepository addressRepository;
    private final PatientRepository patientRepository;
    private final AddressMapper addressMapper;

    @Transactional(readOnly = true)
    public PageResponse<AddressResponse> getPatientAddresses(UUID clinicId, UUID patientId, Pageable pageable) {
        verifyPatientBelongsToClinic(patientId, clinicId);

        Page<Address> addressPage = addressRepository.findByPatientId(patientId, pageable);
        List<AddressResponse> addresses = addressPage.getContent().stream()
                .map(addressMapper::toDto)
                .collect(Collectors.toList());

        return new PageResponse<>(
                addresses,
                pageable.getPageNumber(),
                pageable.getPageSize(),
                addressPage.getTotalElements(),
                addressPage.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public AddressResponse getPatientAddressById(UUID clinicId, UUID patientId, UUID addressId) {
        verifyPatientBelongsToClinic(patientId, clinicId);

        Address address = addressRepository.findByPatientIdAndId(patientId, addressId)
                .orElseThrow(() -> new AddressNotFoundException("Address not found with ID: " + addressId));

        return addressMapper.toDto(address);
    }

    @Transactional
    public AddressResponse createPatientAddress(UUID clinicId, UUID patientId, AddressRequest request) {
        verifyPatientBelongsToClinic(patientId, clinicId);

        Address address = addressMapper.toEntity(request);
        address.setPatientId(patientId);

        if (request.isPrimary()) {
            resetPrimaryFlag(patientId, request.getType());
        }

        Address savedAddress = addressRepository.save(address);
        return addressMapper.toDto(savedAddress);
    }

    @Transactional
    public AddressResponse updatePatientAddress(UUID clinicId, UUID patientId, UUID addressId, AddressRequest request) {
        verifyPatientBelongsToClinic(patientId, clinicId);

        Address existingAddress = addressRepository.findByPatientIdAndId(patientId, addressId)
                .orElseThrow(() -> new AddressNotFoundException("Address not found with ID: " + addressId));

        boolean becomingPrimary = !existingAddress.isPrimary() && request.isPrimary();

        addressMapper.updateEntityFromDto(request, existingAddress);

        if (becomingPrimary) {
            resetPrimaryFlag(patientId, request.getType());
        }

        Address updatedAddress = addressRepository.save(existingAddress);
        return addressMapper.toDto(updatedAddress);
    }

    @Transactional
    public void deletePatientAddress(UUID clinicId, UUID patientId, UUID addressId) {
        verifyPatientBelongsToClinic(patientId, clinicId);

        Address address = addressRepository.findByPatientIdAndId(patientId, addressId)
                .orElseThrow(() -> new AddressNotFoundException("Address not found with ID: " + addressId));

        addressRepository.delete(address);
    }

    private void verifyPatientBelongsToClinic(UUID patientId, UUID clinicId) {
        if (!patientRepository.findByIdAndClinicId(patientId, clinicId).isPresent()) {
            throw new PatientNotFoundException("Patient not found with ID: " + patientId);
        }
    }

    private void resetPrimaryFlag(UUID patientId, AddressType type) {
        addressRepository.resetPrimaryFlags(patientId, type);
    }
}