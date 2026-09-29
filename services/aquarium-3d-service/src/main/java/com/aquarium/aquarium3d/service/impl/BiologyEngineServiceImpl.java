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

    /** Tải sinh học tối đa = 80% thể tích bể (heuristic đơn giản). */
    private static final double BIOLOAD_CAPACITY_PER_LITER = 0.8;

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
        List<Integer> speciesIds = request.getSpeciesIds().stream().filter(Objects::nonNull).distinct().toList();
        double tankVolume = request.getTankVolumeLiters();
        Map<Integer, Integer> quantities = request.getQuantities() != null ? request.getQuantities() : Map.of();

        Map<Integer, BiologicalRule> rules = new HashMap<>();
        biologicalRuleRepository.findAllById(speciesIds).forEach(rule -> rules.put(rule.getId(), rule));

        List<String> warnings = new ArrayList<>();
        boolean compatible = true;
        double totalBioload = 0.0;
        double minPh = 0.0;
        double maxPh = 14.0;
        double minTemp = 0.0;
        double maxTemp = 40.0;

        for (Integer speciesId : speciesIds) {
            BiologicalRule rule = rules.get(speciesId);
            if (rule == null) {
                warnings.add("Không có dữ liệu sinh học cho loài có mã " + speciesId + " — chưa thể đánh giá.");
                continue;
            }
            int schoolMin = rule.getSchoolMinQuantity() != null ? Math.max(rule.getSchoolMinQuantity(), 1) : 1;
            Integer requested = quantities.get(speciesId);
            int quantity = requested != null ? requested : schoolMin;

            if (tankVolume < rule.getMinTankLiters().doubleValue()) {
                warnings.add("Loài '" + rule.getSpeciesName() + "' cần bể tối thiểu " + rule.getMinTankLiters()
                        + "L (bể hiện tại: " + tankVolume + "L).");
                compatible = false;
            }
            if (requested != null && requested < schoolMin) {
                warnings.add("Loài '" + rule.getSpeciesName() + "' nên nuôi theo đàn từ " + schoolMin
                        + " con trở lên (hiện có " + requested + ") để tránh stress.");
            }

            double factor = rule.getBioloadFactor() != null ? rule.getBioloadFactor().doubleValue() : 1.0;
            totalBioload += factor * quantity;

            minPh = Math.max(minPh, rule.getPhMin().doubleValue());
            maxPh = Math.min(maxPh, rule.getPhMax().doubleValue());
            minTemp = Math.max(minTemp, rule.getTempMin().doubleValue());
            maxTemp = Math.min(maxTemp, rule.getTempMax().doubleValue());
        }

        if (minPh > maxPh) {
            warnings.add("Xung đột môi trường nước: khoảng pH phù hợp của các loài không giao nhau (" + minPh + " > " + maxPh + ").");
            compatible = false;
        }
        if (minTemp > maxTemp) {
            warnings.add("Xung đột nhiệt độ: khoảng nhiệt độ phù hợp của các loài không giao nhau (" + minTemp + "°C > " + maxTemp + "°C).");
            compatible = false;
        }

        List<Integer> known = speciesIds.stream().filter(rules::containsKey).toList();
        for (int i = 0; i < known.size(); i++) {
            for (int j = i + 1; j < known.size(); j++) {
                Optional<SpeciesCompatibility> pair = speciesCompatibilityRepository.findCompatibility(known.get(i), known.get(j));
                if (pair.isPresent() && !Boolean.TRUE.equals(pair.get().getIsCompatible())) {
                    warnings.add("Xung đột sinh học: " + pair.get().getConflictReason());
                    compatible = false;
                }
            }
        }

        double capacity = tankVolume * BIOLOAD_CAPACITY_PER_LITER;
        boolean bioLoadSafe = totalBioload <= capacity;
        if (!bioLoadSafe) {
            warnings.add("Mật độ sinh vật quá cao (" + Math.round(totalBioload) + " / " + Math.round(capacity)
                    + "), nguy cơ đục nước hoặc bùng phát rêu hại.");
        }

        return CompatibilityCheckResponse.builder()
                .compatible(compatible && bioLoadSafe)
                .totalBioLoad(Math.round(totalBioload * 10.0) / 10.0)
                .maxBioLoadCapacity(Math.round(capacity * 10.0) / 10.0)
                .bioLoadSafe(bioLoadSafe)
                .warnings(warnings)
                .recommendedPhRange(minPh <= maxPh ? minPh + " - " + maxPh : "Không tương thích")
                .recommendedTempRange(minTemp <= maxTemp ? minTemp + "°C - " + maxTemp + "°C" : "Không tương thích")
                .build();
    }
}
