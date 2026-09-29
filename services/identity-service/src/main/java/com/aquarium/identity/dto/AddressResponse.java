package com.aquarium.identity.dto;

import com.aquarium.identity.entity.UserAddress;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressResponse {

    private UUID id;
    private String recipientName;
    private String phone;
    private String addressLine;
    private String ward;
    private String district;
    private String city;
    private Boolean isDefault;
    private Instant createdAt;

    public static AddressResponse fromEntity(UserAddress address) {
        return AddressResponse.builder()
                .id(address.getId())
                .recipientName(address.getRecipientName())
                .phone(address.getPhone())
                .addressLine(address.getAddressLine())
                .ward(address.getWard())
                .district(address.getDistrict())
                .city(address.getCity())
                .isDefault(Boolean.TRUE.equals(address.getIsDefault()))
                .createdAt(address.getCreatedAt())
                .build();
    }
}
