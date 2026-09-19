package com.aquarium.aquarium3d.service.impl;

import com.aquarium.aquarium3d.dto.CompatibilityCheckRequest;
import com.aquarium.aquarium3d.dto.CompatibilityCheckResponse;
import com.aquarium.aquarium3d.entity.BiologicalRule;
import com.aquarium.aquarium3d.entity.SpeciesCompatibility;
import com.aquarium.aquarium3d.repository.BiologicalRuleRepository;
import com.aquarium.aquarium3d.repository.SpeciesCompatibilityRepository;
import com.aquarium.aquarium3d.service.BiologyEngineService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class BiologyEngineServiceImpl implements BiologyEngineService {

    private final BiologicalRuleRepository biologicalRuleRepository;
    private final SpeciesCompatibilityRepository speciesCompatibilityRepository;

    @Override
    @Transactional(readOnly = true)
    public List<BiologicalRule> getAllRules() {
        return biologicalRuleRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public CompatibilityCheckResponse checkCompatibility(CompatibilityCheckRequest request) {
        List<Integer> speciesIds = request.getSpeciesIds();
        Double tankVolume = request.getTankVolumeLiters();

        List<BiologicalRule> selectedRules = biologicalRuleRepository.findAllById(speciesIds);
        List<String> warnings = new ArrayList<>();
        boolean isCompatible = true;

        double totalBioload = 0.0;
        double minPhRequired = 0.0;
        double maxPhRequired = 14.0;
        double minTempRequired = 0.0;
        double maxTempRequired = 40.0;

        for (BiologicalRule rule : selectedRules) {
            // Check minimum tank volume
            if (tankVolume < rule.getMinTankLiters().doubleValue()) {
                warnings.add("Cảnh báo: Loài '" + rule.getSpeciesName() + "' yêu cầu thể tích bể tối thiểu " 
                        + rule.getMinTankLiters() + "L (Bể hiện tại: " + tankVolume + "L)");
                isCompatible = false;
            }

            // Accumulate bio-load
            totalBioload += rule.getBioloadFactor().doubleValue() * 5.0; // Default estimate 5 specimens

            // Intersect pH range
            minPhRequired = Math.max(minPhRequired, rule.getPhMin().doubleValue());
            maxPhRequired = Math.min(maxPhRequired, rule.getPhMax().doubleValue());

            // Intersect Temp range
            minTempRequired = Math.max(minTempRequired, rule.getTempMin().doubleValue());
            maxTempRequired = Math.min(maxTempRequired, rule.getTempMax().doubleValue());
        }

        // Check pH overlap
        if (minPhRequired > maxPhRequired) {
            warnings.add("Xung đột môi trường nước: Biên độ pH yêu cầu giữa các loài không giao nhau (" 
                    + minPhRequired + " > " + maxPhRequired + ")");
            isCompatible = false;
        }

        // Check Temperature overlap
        if (minTempRequired > maxTempRequired) {
            warnings.add("Xung đột nhiệt độ: Nhiệt độ thích hợp giữa các loài không tương thích (" 
                    + minTempRequired + "°C > " + maxTempRequired + "°C)");
            isCompatible = false;
        }

        // Check Pairwise compatibility in database
        for (int i = 0; i < speciesIds.size(); i++) {
            for (int j = i + 1; j < speciesIds.size(); j++) {
                Optional<SpeciesCompatibility> compat = speciesCompatibilityRepository.findCompatibility(speciesIds.get(i), speciesIds.get(j));
                if (compat.isPresent() && !compat.get().getIsCompatible()) {
                    warnings.add("Xung đột sinh học: " + compat.get().getConflictReason());
                    isCompatible = false;
                }
            }
        }

        double maxBioCapacity = tankVolume * 0.8;
        boolean isBioSafe = totalBioload <= maxBioCapacity;
        if (!isBioSafe) {
            warnings.add("Cảnh báo tải sinh học (Bio-load): Mật độ sinh vật quá cao (" 
                    + Math.round(totalBioload) + " / " + Math.round(maxBioCapacity) + " capacity), nguy cơ làm đục nước hoặc bùng phát rêu hại.");
        }

        String phRange = (minPhRequired <= maxPhRequired) ? (minPhRequired + " - " + maxPhRequired) : "Không tương thích";
        String tempRange = (minTempRequired <= maxTempRequired) ? (minTempRequired + "°C - " + maxTempRequired + "°C") : "Không tương thích";

        return CompatibilityCheckResponse.builder()
                .isCompatible(isCompatible && isBioSafe)
                .totalBioLoad(Math.round(totalBioload * 10.0) / 10.0)
                .maxBioLoadCapacity(Math.round(maxBioCapacity * 10.0) / 10.0)
                .isBioLoadSafe(isBioSafe)
                .warnings(warnings)
                .recommendedPhRange(phRange)
                .recommendedTempRange(tempRange)
                .build();
    }
}
