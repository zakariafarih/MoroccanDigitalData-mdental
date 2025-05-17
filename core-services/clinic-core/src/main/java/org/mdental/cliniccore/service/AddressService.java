package org.mdental.cliniccore.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mdental.cliniccore.event.AddressEvent;
import org.mdental.cliniccore.mapper.AddressMapper;
import org.mdental.cliniccore.model.dto.AddressRequest;
import org.mdental.cliniccore.model.entity.Address;
import org.mdental.cliniccore.model.entity.Clinic;
import org.mdental.cliniccore.repository.AddressRepository;
import org.mdental.commons.exception.BaseException;
import org.mdental.commons.model.ErrorCode;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AddressService {

    private final AddressRepository addressRepository;
    private final ClinicService clinicService;
    private final AddressMapper addressMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public List<Address> getAddressByClinicId(UUID clinicId) {
        log.debug("Fetching addresses for clinic ID: {}", clinicId);
        // Verify clinic exists
        clinicService.getClinicById(clinicId);
        return addressRepository.findByClinicId(clinicId);
    }

    @Transactional(readOnly = true)
    public Address getAddressById(UUID id) {
        log.debug("Fetching address with ID: {}", id);
        return addressRepository.findById(id)
                .orElseThrow(() -> new AddressNotFoundException("Address not found with ID: " + id));
    }

    @Transactional
    public Address createAddress(UUID clinicId, AddressRequest request) {
        log.info("Creating new address for clinic ID: {}", clinicId);

        Clinic clinic = clinicService.getClinicById(clinicId);

        // If this is a primary address, update existing primary addresses of the same type
        if (Boolean.TRUE.equals(request.getPrimary())) {
            handlePrimaryAddress(clinicId, request.getType());
        }

        Address address = addressMapper.toEntity(request);
        address.setClinic(clinic);

        Address savedAddress = addressRepository.save(address);

        // Publish event
        eventPublisher.publishEvent(new AddressEvent(
                this,
                savedAddress,
                AddressEvent.EventType.CREATED));

        return savedAddress;
    }

    @Transactional
    public Address updateAddress(UUID id, AddressRequest request) {
        log.info("Updating address with ID: {}", id);

        Address address = getAddressById(id);
        Address originalAddress = copyAddress(address);

        // If this is becoming primary, handle existing primary addresses
        if (Boolean.TRUE.equals(request.getPrimary()) && !Boolean.TRUE.equals(address.getPrimary())) {
            handlePrimaryAddress(address.getClinic().getId(), request.getType());
        }

        address.setType(request.getType());
        address.setStreet(request.getStreet());
        address.setCity(request.getCity());
        address.setState(request.getState());
        address.setZip(request.getZip());
        address.setCountry(request.getCountry());
        address.setLatitude(request.getLatitude());
        address.setLongitude(request.getLongitude());
        address.setPrimary(request.getPrimary());

        Address updatedAddress = addressRepository.save(address);

        // Publish event
        eventPublisher.publishEvent(new AddressEvent(
                this,
                updatedAddress,
                AddressEvent.EventType.UPDATED,
                originalAddress));

        return updatedAddress;
    }

    @Transactional
    public void deleteAddress(UUID id) {
        log.info("Deleting address with ID: {}", id);
        Address address = getAddressById(id);

        // Get current username for soft delete
        String username = getCurrentUsername();
        address.softDelete(username);

        addressRepository.save(address);

        // Publish event
        eventPublisher.publishEvent(new AddressEvent(
                this,
                address,
                AddressEvent.EventType.DELETED));
    }

    /**
     * Helper method to handle primary address - ensures only one address of each type is primary
     */
    private void handlePrimaryAddress(UUID clinicId, Address.AddressType type) {
        Optional<Address> existingPrimary = addressRepository.findPrimaryAddressByType(clinicId, type);
        existingPrimary.ifPresent(addr -> {
            addr.setPrimary(false);
            addressRepository.save(addr);
        });
    }

    /**
     * Creates a copy of the address entity for event comparison
     */
    private Address copyAddress(Address original) {
        Address copy = Address.builder()
                .clinic(original.getClinic())
                .type(original.getType())
                .street(original.getStreet())
                .city(original.getCity())
                .state(original.getState())
                .zip(original.getZip())
                .country(original.getCountry())
                .latitude(original.getLatitude())
                .longitude(original.getLongitude())
                .primary(original.getPrimary())
                .build();
        copy.setId(original.getId());
        return copy;
    }

    /**
     * Gets the current username from request header or "system" if not available
     */
    private String getCurrentUsername() {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String username = request.getHeader("X-User-Username");
                if (username != null && !username.isBlank()) {
                    return username;
                }
            }
        } catch (Exception e) {
            // Fall back to system user
        }

        return "system";
    }

    // Custom exceptions
    public static class AddressNotFoundException extends BaseException {
        public AddressNotFoundException(String message) {
            super(message, ErrorCode.RESOURCE_NOT_FOUND);
        }
    }
}