package com.aquarium.identity.service;

import com.aquarium.common.exception.AppException;
import com.aquarium.common.exception.ErrorCode;
import com.aquarium.identity.dto.AddressRequest;
import com.aquarium.identity.dto.AddressResponse;
import com.aquarium.identity.entity.UserAddress;
import com.aquarium.identity.repository.UserAddressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Sổ địa chỉ: mọi thao tác đều gắn với chủ sở hữu lấy từ JWT. */
@Service
@RequiredArgsConstructor
public class AddressService {

    static final int MAX_ADDRESSES = 10;

    private final UserAddressRepository addressRepository;

    @Transactional(readOnly = true)
    public List<AddressResponse> list(UUID userId) {
        return addressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId).stream()
                .map(AddressResponse::fromEntity)
                .toList();
    }

    @Transactional
    public AddressResponse create(UUID userId, AddressRequest request) {
        long count = addressRepository.countByUserId(userId);
        if (count >= MAX_ADDRESSES) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Sổ địa chỉ chỉ lưu tối đa " + MAX_ADDRESSES + " địa chỉ");
        }
        boolean makeDefault = count == 0 || Boolean.TRUE.equals(request.getIsDefault());
        if (makeDefault) {
            addressRepository.clearDefault(userId);
        }
        UserAddress address = UserAddress.builder().userId(userId).isDefault(makeDefault).build();
        apply(address, request);
        return AddressResponse.fromEntity(addressRepository.save(address));
    }

    @Transactional
    public AddressResponse update(UUID userId, UUID addressId, AddressRequest request) {
        UserAddress address = find(userId, addressId);
        boolean makeDefault = Boolean.TRUE.equals(request.getIsDefault()) && !Boolean.TRUE.equals(address.getIsDefault());
        if (makeDefault) {
            addressRepository.clearDefault(userId);
            address = find(userId, addressId);
            address.setIsDefault(true);
        }
        apply(address, request);
        return AddressResponse.fromEntity(addressRepository.save(address));
    }

    @Transactional
    public void delete(UUID userId, UUID addressId) {
        UserAddress address = find(userId, addressId);
        boolean wasDefault = Boolean.TRUE.equals(address.getIsDefault());
        addressRepository.delete(address);
        addressRepository.flush();
        if (wasDefault) {
            addressRepository.findFirstByUserIdOrderByCreatedAtDesc(userId).ifPresent(next -> {
                next.setIsDefault(true);
                addressRepository.save(next);
            });
        }
    }

    @Transactional
    public AddressResponse setDefault(UUID userId, UUID addressId) {
        find(userId, addressId);
        addressRepository.clearDefault(userId);
        UserAddress address = find(userId, addressId);
        address.setIsDefault(true);
        return AddressResponse.fromEntity(addressRepository.save(address));
    }

    private UserAddress find(UUID userId, UUID addressId) {
        return addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy địa chỉ"));
    }

    private static void apply(UserAddress address, AddressRequest request) {
        address.setRecipientName(request.getRecipientName().trim());
        address.setPhone(request.getPhone().trim());
        address.setAddressLine(request.getAddressLine().trim());
        address.setWard(trimToNull(request.getWard()));
        address.setDistrict(request.getDistrict().trim());
        address.setCity(request.getCity().trim());
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
